package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.exception.AlreadyEnrolledException;
import com.nhnacademy.book.exception.BookNotFoundException;
import com.nhnacademy.book.exception.CategoryNotFoundException;
import com.nhnacademy.book.repository.*;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
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

    @Override
    @Transactional(readOnly = true)
    public Page<BookListResponse> getBooks(Pageable pageable) {
        // 전체 조회는 쿼리 단계에서 이미지를 같이 가져오므로 그대로 유지
        Page<Object[]> results = bookRepository.findAllBooksWithImage(pageable, FileType.BOOK);
        return results.map(row -> {
            Book book = (Book) row[0];
            String imageUrl = (String) row[1];
            return BookListResponse.from(book, imageUrl);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookListResponse> getBooksByIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return List.of();
        List<Book> books = bookRepository.findAllById(bookIds);
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getBookId, book -> book));
        List<Book> sortedBooks = bookIds.stream().filter(bookMap::containsKey).map(bookMap::get).collect(Collectors.toList());
        return convertToDtoList(sortedBooks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookListResponse> getPopularBooks() {
        List<Book> books = bookRepository.findTop10ByOrderByViewCountDesc();
        return convertToDtoList(books);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookListResponse> getNewBooks() {
        // 전체 신간 Top 5
        Page<Book> booksPage = bookRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "bookPublicationDate")));
        return convertToDtoList(booksPage.getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookListResponse> getBooksByCategory(Long categoryId) {
        // [★필수] 카테고리별 신간 Top 5 구현
        Pageable limitFive = PageRequest.of(0, 5);
        List<Book> books = bookRepository.findBooksByCategoryId(categoryId, limitFive);
        return convertToDtoList(books);
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("해당 도서가 존재하지 않습니다. ID: " + bookId));
        String imageUrl = fileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK)
                .map(BookFile::getFileUrl).orElse(null);
        return BookDetailResponse.from(book, imageUrl);
    }

    // ... (create, update, delete 메서드는 기존 코드 유지) ...
    @Override
    @Transactional
    public Long createBook(BookCreateRequest request, MultipartFile file) {
        if(bookRepository.existsBookByIsbn(request.isbn())){ throw new AlreadyEnrolledException("이미 등록된 도서입니다."); }
        String uploadUrl = null;
        if (file != null && !file.isEmpty()) { uploadUrl = minioService.uploadImage(file); }
        else if (request.bookImage() != null && !request.bookImage().isBlank()) { uploadUrl = minioService.uploadFromUrl(request.bookImage()); }

        Publisher publisher = null;
        if (request.bookPublisher() != null && !request.bookPublisher().isBlank()) {
            String publisherName = request.bookPublisher().trim();
            publisher = publisherRepository.findByPublisherName(publisherName).orElseGet(() -> publisherRepository.save(new Publisher(publisherName)));
        }

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
                .publisher(publisher)
                .build();

        processAuthors(book, request.bookAuthor());
        processTags(book, request.tags());
        processCategories(book, request.categoryIdList());

        Book savedBook = bookRepository.save(book);
        String finalImageUrl = (uploadUrl != null) ? uploadUrl : request.bookImage();
        if (finalImageUrl != null && !finalImageUrl.isBlank()) { fileService.saveBookImage(savedBook.getBookId(), finalImageUrl); }
        return savedBook.getBookId();
    }

    @Override
    @Transactional
    public void updateBook(Long bookId, BookUpdateRequest request) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException("수정할 도서가 없습니다. ID: " + bookId));
        int newSalePrice = calculateSalePrice(book.getBookRegularPrice(), request.discountRate());
        book.updateBookInfo(request.bookName(), request.bookDescription(), request.bookIndex(), request.bookPackaging(), request.bookState(), request.bookStock(), newSalePrice);
        if (request.bookImage() != null && !request.bookImage().isBlank()) {
            Optional<BookFile> existingFile = fileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK);
            if (existingFile.isPresent()) { existingFile.get().setFileUrl(request.bookImage()); }
            else { fileService.saveBookImage(bookId, request.bookImage()); }
        }
    }

    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException("삭제할 도서가 없습니다. ID: " + bookId));
        book.markAsSoldOut();
    }

    @Override
    @Transactional
    public void increaseViewCount(Long bookId) {
        bookRepository.updateViewCount(bookId);
    }

    @Override
    public int calculateSalePrice(int regularPrice, double discountRate) {
        return (int) Math.round(regularPrice * (1 - discountRate / 100.0));
    }

    // [중요] 도서 목록을 DTO로 변환하면서 이미지 URL을 매핑하는 메서드
    private List<BookListResponse> convertToDtoList(List<Book> books) {
        if (books.isEmpty()) return List.of();
        List<Long> bookIds = books.stream().map(Book::getBookId).toList();
        List<BookFile> images = fileRepository.findAllByJoinedIdInAndFileType(bookIds, FileType.BOOK);
        // Map<BookId, ImageUrl> 생성
        Map<Long, String> imageMap = images.stream().collect(Collectors.toMap(BookFile::getJoinedId, BookFile::getFileUrl, (oldVal, newVal) -> oldVal));

        return books.stream().map(book -> BookListResponse.from(book, imageMap.get(book.getBookId()))).collect(Collectors.toList());
    }

    private void processAuthors(Book book, String authorStr) {
        if (authorStr != null && !authorStr.isBlank()) {
            for (String name : authorStr.split(",")) {
                String cleanName = name.replaceAll("\\(.*?\\)", "").trim();
                if (!cleanName.isEmpty()) {
                    Author author = authorRepository.findByAuthorName(cleanName).orElseGet(() -> authorRepository.save(new Author(cleanName)));
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
                    Tag tag = tagRepository.findByTagName(cleanTagName).orElseGet(() -> tagRepository.save(new Tag(cleanTagName)));
                    book.getBookTags().add(new BookTag(tag, book));
                }
            }
        }
    }

    private void processCategories(Book book, java.util.List<Long> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long catId : categoryIds) {
                Category category = categoryRepository.findById(catId).orElseThrow(() -> new CategoryNotFoundException("존재하지 않는 카테고리 ID: " + catId));
                book.getBookCategories().add(new BookCategory(category, book));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getRootCategories() {
        List<Category> rootCategories = categoryRepository.findAllByParentIsNull();
        return rootCategories.stream().map(category -> new CategoryTreeResponse(category.getCategoryId(), category.getCategoryName(), List.of())).collect(Collectors.toList());
    }

    // [수정 완료] 카테고리별 도서 조회 시에도 이미지를 함께 가져오도록 변경
    @Override
    @Transactional(readOnly = true)
    public Page<BookListResponse> getBooksByCategoryPage(Long categoryId, Pageable pageable) {
        // 1. 카테고리에 해당하는 도서 페이징 조회 (엔티티만 가져옴)
        Page<Book> booksPage = bookRepository.findByBookCategories_Category_CategoryId(categoryId, pageable);

        // 2. 조회된 도서들의 ID 추출
        List<Long> bookIds = booksPage.getContent().stream()
                .map(Book::getBookId)
                .collect(Collectors.toList());

        // 3. 도서 ID들에 해당하는 이미지 조회
        List<BookFile> images = fileRepository.findAllByJoinedIdInAndFileType(bookIds, FileType.BOOK);
        Map<Long, String> imageMap = images.stream()
                .collect(Collectors.toMap(BookFile::getJoinedId, BookFile::getFileUrl, (oldVal, newVal) -> oldVal));

        // 4. 도서 엔티티와 이미지 URL을 조립하여 DTO로 변환
        return booksPage.map(book -> BookListResponse.from(book, imageMap.get(book.getBookId())));
    }
}