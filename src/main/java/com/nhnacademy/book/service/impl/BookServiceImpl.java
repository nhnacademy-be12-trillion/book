package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.repository.*;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final FileService fileService;
    private final MinioService minioService;
    private final AuthorRepository authorRepository;       // [추가] 작가 조회용
    private final PublisherRepository publisherRepository; // [추가] 출판사 조회용
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;

    // 도서 목록 조회 구현 (BookListResponse 사용)
    @Override
    public Page<BookListResponse> getBooks(Pageable pageable) {
        // JPA로 모든 Entity를 가져온 후, List DTO로 변환하여 반환
        return bookRepository.findAll(pageable)
                .map(BookListResponse::from);
    }

    // 도서 상세 조회 구현 (BookDetailResponse 사용)
    @Override
    public BookDetailResponse getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 도서가 존재하지 않습니다. ID: " + bookId));

        // Entity를 Detail DTO로 변환하여 반환
        return BookDetailResponse.from(book);
    }

    @Override
    @Transactional
    public Long createBook(BookCreateRequest request, MultipartFile file) {
        Book book = new Book();

        // 1. 이미지 업로드 처리
        String uploadUrl = null;

        if (file != null && !file.isEmpty()) {
            log.info("파일 업로드 감지: MinIO로 직접 업로드 시도");
            uploadUrl = minioService.uploadImage(file);
        } else if (request.bookImage() != null && !request.bookImage().isBlank()) {
            log.info("이미지 URL 감지: 서버에서 다운로드 및 업로드 시도 -> {}", request.bookImage());
            uploadUrl = minioService.uploadFromUrl(request.bookImage());
        }

        //도서 등록시 기본정보 매핑. -> 도서 엔티티 기반
        book.setIsbn(request.isbn());
        book.setBookName(request.bookName());
        book.setBookDescription(request.bookDescription());
        book.setBookPublicationDate(request.bookPublicationDate());
        book.setBookIndex(request.bookIndex());
        book.setBookPackaging(request.bookPackaging());
        book.setBookState(request.bookState());
        book.setBookStock(request.bookStock());
        book.setBookRegularPrice(request.bookRegularPrice());
        book.setBookSalePrice(request.bookSalePrice());
        book.setBookReviewRate(0.0);

        //가져온 도서 이미지가 업로드가 성공했다면 minIO url을 실패시 기본 알라딘 이미지를 저장
        if (uploadUrl != null) {
            book.setBookImage(uploadUrl);
        } else {
            book.setBookImage(request.bookImage());
        }

        //도서를 등록할때 가져온 출판사의 정보가 이미 Db에 있다면?
        if (request.bookPublisher() != null && !request.bookPublisher().isBlank()) {
            String publisherName = request.bookPublisher().trim();

            //db에서 출판사 정보 조회
            Optional<Publisher> existingPublisher = publisherRepository.findByPublisherName(publisherName);
            Publisher publisher;

            if (existingPublisher.isPresent()) {
                log.info("기존 출판사 발견: '{}' -> ID: {}", publisherName, existingPublisher.get().getPublisherId());
                publisher = existingPublisher.get();
                //db에 존재하지 않는 출판사였다면? -> 새롭게 출판사 정보를 만들고 도서에 할당.
            } else {
                log.info("새로운 출판사 생성: '{}'", publisherName);
                publisher = publisherRepository.save(new Publisher(publisherName));
            }

            book.setPublisher(publisher);
        }

        //작가 정보
        String authorStr = request.bookAuthor();

        //작가 정보가 이미 db에 있다면?
        if (authorStr != null && !authorStr.isBlank()) {
            String[] authorNames = authorStr.split(",");

            for (String name : authorNames) {
                String cleanName = name.replaceAll("\\(.*?\\)", "").trim();

                if (!cleanName.isEmpty()) {
                    // DB에서 조회
                    Optional<Author> existingAuthor = authorRepository.findByAuthorName(cleanName);
                    Author author;

                    if (existingAuthor.isPresent()) {
                        log.info(" 기존 작가 발견: '{}' -> ID: {}", cleanName, existingAuthor.get().getAuthorId());
                        author = existingAuthor.get();
                        //없는 경우 새로운 작가 정보를 Author테이블에 등록
                    } else {
                        log.info(" 새로운 작가 생성: '{}'", cleanName);
                        author = authorRepository.save(new Author(cleanName));
                    }
                    BookAuthor bookAuthor = new BookAuthor(author, book);
                    book.getBookAuthors().add(bookAuthor);
                }
            }
            String tagStr = request.tags();
            if (tagStr != null && !tagStr.isBlank()) {
                String[] tagNames = tagStr.split(",");
                for (String name : tagNames) {
                    String cleanTagName = name.trim();
                    if (!cleanTagName.isEmpty()) {
                        Tag tag = tagRepository.findByTagName(cleanTagName)
                                .orElseGet(() -> {
                                    log.info("새로운 태그 생성: '{}'", cleanTagName);
                                    return tagRepository.save(new Tag(cleanTagName));
                                });

                        // BookTag 연결
                        book.getBookTags().add(new BookTag(tag, book));
                    }
                }
            }
            if (request.categoryIdList() != null && !request.categoryIdList().isEmpty()) {
                for (Long catId : request.categoryIdList()) {
                    Category category = categoryRepository.findById(catId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID: " + catId));

                    // 카테고리 하나당 BookCategory 하나씩 생성해서 추가
                    BookCategory bookCategory = new BookCategory(category, book);
                    book.getBookCategories().add(bookCategory);
                }
            }

        }

        // 3. 최종 저장
        Book savedBook = bookRepository.save(book);

        if (uploadUrl != null) {
            fileService.saveBookImage(savedBook.getBookId(), uploadUrl);
        }

        log.info("🎉 도서 등록 완료! ID: {}, 제목: {}", savedBook.getBookId(), savedBook.getBookName());
        return savedBook.getBookId();
    }

    // 도서 정보 수정 (Update Request DTO 사용)
    @Override
    @Transactional // 쓰기 작업이므로 트랜잭션 필요
    public void updateBook(Long bookId, BookUpdateRequest request) {

        // 수정할 Entity를 DB에서 조회 (없으면 예외 발생)
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 도서가 없습니다. ID: " + bookId));

        // 할인율을 적용하여 판매가 계산
        int regularPrice = book.getBookRegularPrice();
        double discountRate = request.discountRate();

        // 판매가 계산: 정가 * (1 - 할인율/100)
        int newSalePrice = calculateSalePrice(regularPrice, discountRate);

        // 요청받은 값과 계산된 판매가로 Entity 필드 수정
        // (JPA의 변경 감지(Dirty Checking) 기능으로 자동 저장)
        book.setBookName(request.bookName());
        book.setBookDescription(request.bookDescription());
        book.setBookIndex(request.bookIndex());
        book.setBookPackaging(request.bookPackaging());
        book.setBookState(request.bookState());
        book.setBookStock(request.bookStock());
        book.setBookImage(request.bookImage());

        // 계산된 판매가 반영
        book.setBookSalePrice(newSalePrice);

        // @Transactional 메서드가 끝날 때, 변경된 내용이 자동으로 DB에 반영
    }

    // 도서 삭제 (물리적 삭제 대신 bookState를 SALE_END로 변경)
    @Override
    @Transactional // 상태를 변경하는 쓰기 작업이므로 @Transactional 필요
    public void deleteBook(Long bookId) {

        // 삭제할 Entity를 DB에서 조회 (없으면 예외 발생)
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 도서가 없습니다. ID: " + bookId));

        // BookState를 '판매 종료' 상태로 변경
        book.setBookState(BookState.SALE_END);

        // @Transactional이 설정되어 있으므로, 이 시점에 변경 감지(Dirty Checking)를 통해
        // 별도로 save()를 호출하지 않아도 DB에 상태 자동 반영
    }

    public void increaseViewCount(Long bookId) {
        bookRepository.updateViewCount(bookId);
    }

    @Override
    @Transactional
    public void deductStock(Long bookId, int quantity) {
        // 책 가져오기
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("책이 없습니다."));

        // 엔티티에게 "재고 깎아" 시키기 (상태 변경 로직은 엔티티 안에 있으니 알아서 됨)
        book.deductStock(quantity);
    }

    @Override
    @Transactional
    public int calculateSalePrice(int regularPrice, double discountRate) {
        return (int) Math.round(regularPrice * (1 - discountRate / 100.0));
    }
}