package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.exception.AlreadyEnrolledException;
import com.nhnacademy.book.exception.BookNotFoundException;
import com.nhnacademy.book.exception.CategoryNotFoundException;
import com.nhnacademy.book.repository.*;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final FileService fileService;
    private final BookFileRepository fileRepository;
    private final MinioService minioService;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;

    // 도서 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<BookListResponse> getBooks(Pageable pageable) {
        // Object 타입은 어떤 걸 가져오는지 너무 불명확함 수정 필요
        Page<Object[]> results = bookRepository.findAllBooksWithImage(pageable, FileType.BOOK);

        return results.map(row -> {
            Book book = (Book) row[0];
            String imageUrl = (String) row[1];
            return BookListResponse.from(book, imageUrl);
        });
    }
    // 조회수 많은 도서 5개 조회
    @Override
    @Transactional(readOnly = true)
    public List<BookListResponse> getPopularBooks() {
        List<Book> books = bookRepository.findTop5ByOrderByViewCountDesc();
        return convertToDtoList(books);
    }

    // 카테고리별 신간 5개 조회
    @Override
    @Transactional(readOnly = true)
    public List<BookListResponse> getBooksByCategory(Long categoryId) {
        // 0페이지에서 5개만 가져오라 (LIMIT 5 효과)
        Pageable limitFive = PageRequest.of(0, 5);
        List<Book> books = bookRepository.findBooksByCategoryId(categoryId, limitFive);
        return convertToDtoList(books);
    }

    // 도서 상세 조회
    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("해당 도서가 존재하지 않습니다. ID: " + bookId));

        String imageUrl = fileRepository.findFirstByJoinedIdAndFileType(bookId,FileType.BOOK)
                .map(BookFile::getFileUrl)
                .orElse(null);

        return BookDetailResponse.from(book, imageUrl);
    }

    // 도서 등록 (Builder 적용)
    @Override
    @Transactional
    public Long createBook(BookCreateRequest request, MultipartFile file) {

        if(bookRepository.existsBookByIsbn(request.isbn())){
            throw new AlreadyEnrolledException("이미 등록된 도서입니다.");
        }

        // 이미지 업로드 처리
        String uploadUrl = null;
        if (file != null && !file.isEmpty()) {
            log.info("파일 업로드 감지: MinIO로 직접 업로드 시도");
            uploadUrl = minioService.uploadImage(file);
        } else if (request.bookImage() != null && !request.bookImage().isBlank()) {
            log.info("이미지 URL 감지: 서버에서 다운로드 및 업로드 시도 -> {}", request.bookImage());
            uploadUrl = minioService.uploadFromUrl(request.bookImage());
        }

        // 출판사 처리
        Publisher publisher = null;
        if (request.bookPublisher() != null && !request.bookPublisher().isBlank()) {
            String publisherName = request.bookPublisher().trim();
            publisher = publisherRepository.findByPublisherName(publisherName)
                    .orElseGet(() -> {
                        log.info("새로운 출판사 생성: '{}'", publisherName);
                        return publisherRepository.save(new Publisher(publisherName));
                    });
        }

        // Builder로 Book 객체 생성
        Book book = Book.builder()
                .isbn(request.isbn())
                .bookName(request.bookName())
                .bookDescription(request.bookDescription())
                .bookPublicationDate(request.bookPublicationDate())
                .bookIndex(request.bookIndex())
                .bookPackaging(request.bookPackaging())
                .bookState(request.bookState())
                .bookStock(request.bookStock())
                .bookRegularPrice(request.bookRegularPrice())
                .bookSalePrice(request.bookSalePrice())
                .bookReviewRate(0.0)
                .publisher(publisher) // 찾은 출판사 바로 주입
                .build();

        // 작가, 태그, 카테고리 처리 (Getter로 컬렉션 가져와서 add)
        processAuthors(book, request.bookAuthor());
        processTags(book, request.tags());
        processCategories(book, request.categoryIdList());

        // 책 저장
        Book savedBook = bookRepository.save(book);

        // 이미지 저장 (BookFile 테이블 사용)
        String finalImageUrl = (uploadUrl != null) ? uploadUrl : request.bookImage();
        if (finalImageUrl != null && !finalImageUrl.isBlank()) {
            fileService.saveBookImage(savedBook.getBookId(), finalImageUrl);
        }

        log.info("도서 등록 완료 ID: {}, 제목: {}", savedBook.getBookId(), savedBook.getBookName());
        return savedBook.getBookId();
    }

    // 도서 수정
    @Override
    @Transactional
    public void updateBook(Long bookId, BookUpdateRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("수정할 도서가 없습니다. ID: " + bookId));

        int newSalePrice = calculateSalePrice(book.getBookRegularPrice(), request.discountRate());

        book.updateBookInfo(
                request.bookName(),
                request.bookDescription(),
                request.bookIndex(),
                request.bookPackaging(),
                request.bookState(),
                request.bookStock(),
                newSalePrice
        );

        // 이미지 수정 (BookFile 업데이트)
        if (request.bookImage() != null && !request.bookImage().isBlank()) {
            Optional<BookFile> existingFile = fileRepository.findFirstByJoinedIdAndFileType(bookId,FileType.BOOK);
            if (existingFile.isPresent()) {
                existingFile.get().setFileUrl(request.bookImage());
            } else {
                fileService.saveBookImage(bookId, request.bookImage());
            }
        }
    }

    // 도서 삭제
    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("삭제할 도서가 없습니다. ID: " + bookId));

        book.markAsSoldOut();
    }

    @Override
    @Transactional
    public void increaseViewCount(Long bookId) {
        bookRepository.updateViewCount(bookId);
    }

//    @Override
//    @Transactional
//    public void deductStock(Long bookId, int quantity) {
//        Book book = bookRepository.findById(bookId)
//                .orElseThrow(() -> new BookNotFoundException("해당 도서를 찾을 수 없습니다."));
//        book.deductStock(quantity);
//    }

    @Override
    public int calculateSalePrice(int regularPrice, double discountRate) {
        return (int) Math.round(regularPrice * (1 - discountRate / 100.0));
    }

    private List<BookListResponse> convertToDtoList(List<Book> books) {
        if (books.isEmpty())
            return List.of();

        // 책 ID 추출
        List<Long> bookIds = books.stream().map(Book::getBookId).toList();

        // 이미지 일괄 조회 (IN 쿼리)
        List<BookFile> images = fileRepository.findAllByJoinedIdInAndFileType(bookIds, FileType.BOOK);

        // Map 변환 (BookId -> ImageUrl)
        Map<Long, String> imageMap = images.stream()
                .collect(Collectors.toMap(
                        BookFile::getJoinedId,
                        BookFile::getFileUrl,
                        (oldVal, newVal) -> oldVal // 중복 시 기존 것 유지
                ));

        // DTO 변환
        return books.stream()
                .map(book -> BookListResponse.from(book, imageMap.get(book.getBookId())))
                .collect(Collectors.toList());
    }

    // --- Helper Methods ---
    private void processAuthors(Book book, String authorStr) {
        if (authorStr != null && !authorStr.isBlank()) {
            for (String name : authorStr.split(",")) {
                String cleanName = name.replaceAll("\\(.*?\\)", "").trim();
                if (!cleanName.isEmpty()) {
                    Author author = authorRepository.findByAuthorName(cleanName)
                            .orElseGet(() -> authorRepository.save(new Author(cleanName)));
                    book.getBookAuthors().add(new BookAuthor(author, book));
                }
            }
        }
    }

    private void processTags(Book book, String tagStr) {
        if (tagStr != null && !tagStr.isBlank()) {
            for (String name : tagStr.split(",")) {
                String cleanTagName = name.trim();
                if (!cleanTagName.isEmpty()) {
                    Tag tag = tagRepository.findByTagName(cleanTagName)
                            .orElseGet(() -> tagRepository.save(new Tag(cleanTagName)));
                    book.getBookTags().add(new BookTag(tag, book));
                }
            }
        }
    }

    private void processCategories(Book book, java.util.List<Long> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long catId : categoryIds) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new CategoryNotFoundException("존재하지 않는 카테고리 ID: " + catId));
                book.getBookCategories().add(new BookCategory(category, book));
            }
        }
    }
}