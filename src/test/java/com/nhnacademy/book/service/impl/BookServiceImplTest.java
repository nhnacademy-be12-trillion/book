package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.exception.AlreadyEnrolledException;
import com.nhnacademy.book.exception.BookNotFoundException;
import com.nhnacademy.book.repository.*;
import com.nhnacademy.book.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @InjectMocks
    private BookServiceImpl bookService;

    // Service에서 사용하는 모든 의존성을 Mock으로 선언
    @Mock private BookRepository bookRepository;
    @Mock private FileService fileService;
    @Mock private BookFileRepository fileRepository;
    @Mock private MinioService minioService;
    @Mock private AuthorRepository authorRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private TagRepository tagRepository;
    @Mock private CategoryRepository categoryRepository;

    @Test
    @DisplayName("도서 전체 조회 (페이징 + 이미지 포함) 성공")
    void getBooks_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Book book = createDummyBook(1L);
        String imageUrl = "http://image.url/test.jpg";

        List<Object[]> content = new ArrayList<>();
        content.add(new Object[]{book, imageUrl});

        Page<Object[]> pageResult = new PageImpl<>(content, pageable, 1);

        given(bookRepository.findAllBooksWithImage(pageable, FileType.BOOK)).willReturn(pageResult);

        // when
        Page<BookListResponse> result = bookService.getBooks(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).bookName()).isEqualTo(book.getBookName());
        assertThat(result.getContent().get(0).bookImage()).isEqualTo(imageUrl);
    }

    @Test
    @DisplayName("도서 상세 조회 성공")
    void getBook_Success() {
        // given
        Long bookId = 1L;
        Book book = createDummyBook(bookId);
        String imageUrl = "http://image.url/detail.jpg";
        BookFile bookFile = BookFile.builder().fileUrl(imageUrl).build();

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(fileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK))
                .willReturn(Optional.of(bookFile));

        // when
        BookDetailResponse response = bookService.getBook(bookId);

        // then
        assertThat(response.bookId()).isEqualTo(bookId);
        assertThat(response.bookName()).isEqualTo("Test Book");
        assertThat(response.bookImage()).isEqualTo(imageUrl);
        assertThat(response.discountRate()).isEqualTo(10);
    }

    @Test
    @DisplayName("도서 상세 조회 실패 - 존재하지 않는 ID")
    void getBook_Fail_NotFound() {
        // given
        Long bookId = 999L;
        given(bookRepository.findById(bookId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookService.getBook(bookId))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("해당 도서가 존재하지 않습니다");
    }

    @Test
    @DisplayName("도서 상세 조회 실패 - 판매 종료된 도서")
    void getBook_Fail_SaleEnd() {
        // given
        Long bookId = 1L;
        Book book = Book.builder()
                .bookState(BookState.SALE_END)
                .bookName("Sale End Book")
                .bookRegularPrice(10000)
                .bookSalePrice(9000)
                .build();

        ReflectionTestUtils.setField(book, "bookId", bookId);
        ReflectionTestUtils.setField(book, "bookAuthors", new java.util.HashSet<>());
        ReflectionTestUtils.setField(book, "bookCategories", new java.util.HashSet<>());

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // when & then
        assertThatThrownBy(() -> bookService.getBook(bookId))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("판매가 종료되어 조회할 수 없는 도서입니다");
    }

    @Test
    @DisplayName("도서 생성 성공")
    void createBook_Success() {
        // given
        BookCreateRequest request = new BookCreateRequest(
                "978-1234567890", "New Book", "Desc", "Publisher",
                "Author", "Tag", List.of(1L), LocalDate.now(),
                "Index", true, BookState.ON_SALE, 100, 10000, 9000, "http://image.url"
        );

        given(bookRepository.existsBookByIsbn(anyString())).willReturn(false);

        // 이미지 URL 업로드 동작 정의
        given(minioService.uploadFromUrl(anyString())).willReturn("http://minio.url/image.jpg");

        // Publisher Mock
        given(publisherRepository.findByPublisherName(anyString())).willReturn(Optional.empty());
        given(publisherRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // Author Mock
        given(authorRepository.findByAuthorName(anyString())).willReturn(Optional.empty());
        given(authorRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // Tag Mock
        given(tagRepository.findByTagName(anyString())).willReturn(Optional.empty());
        given(tagRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // Category Mock
        Category category = new Category();
        ReflectionTestUtils.setField(category, "categoryId", 1L);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        // Book Mock
        Book savedBook = createDummyBook(1L);
        given(bookRepository.save(any(Book.class))).willReturn(savedBook);

        // when
        Long resultId = bookService.createBook(request, null);

        // then
        assertThat(resultId).isEqualTo(1L);
        verify(bookRepository).save(any(Book.class));
        verify(fileService).saveBookImage(anyLong(), anyString());
    }

    @Test
    @DisplayName("도서 생성 실패 - 이미 존재하는 ISBN")
    void createBook_Fail_DuplicateIsbn() {
        // given
        BookCreateRequest request = new BookCreateRequest(
                "978-1234567890", "Title", "Desc", "Pub", "Auth", "Tag",
                List.of(), LocalDate.now(), "Idx", true, BookState.ON_SALE,
                10, 1000, 900, null
        );
        given(bookRepository.existsBookByIsbn(request.isbn())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> bookService.createBook(request, null))
                .isInstanceOf(AlreadyEnrolledException.class);
    }

    @Test
    @DisplayName("도서 수정 성공 (가격 및 정보 변경)")
    void updateBook_Success() {
        // given
        Long bookId = 1L;
        Book book = createDummyBook(bookId);
        BookUpdateRequest updateRequest = new BookUpdateRequest(
                "Updated Title", "Updated Desc", "Updated Index",
                false, BookState.SOLD_OUT, 50, 20.0, "new_image.jpg"
        );

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(fileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK))
                .willReturn(Optional.empty());

        // when
        bookService.updateBook(bookId, updateRequest);

        // then
        assertThat(book.getBookName()).isEqualTo("Updated Title");
        assertThat(book.getBookState()).isEqualTo(BookState.SOLD_OUT);
        assertThat(book.getBookSalePrice()).isEqualTo(8000);

        verify(fileService).saveBookImage(bookId, "new_image.jpg");
    }

    @Test
    @DisplayName("도서 삭제 (상태 변경) 성공")
    void deleteBook_Success() {
        // given
        Long bookId = 1L;
        Book book = createDummyBook(bookId);
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // when
        bookService.deleteBook(bookId);

        // then
        assertThat(book.getBookState()).isEqualTo(BookState.SALE_END);
    }

    @Test
    @DisplayName("판매가 계산 로직 테스트")
    void calculateSalePrice_Test() {
        // given
        int regularPrice = 10000;
        double discountRate = 10.0;

        // when
        int salePrice = bookService.calculateSalePrice(regularPrice, discountRate);

        // then
        assertThat(salePrice).isEqualTo(9000);
    }

    @Test
    @DisplayName("ID 리스트로 도서 조회 - 순서 보장 확인")
    void getBooksByIds_Success() {
        // given
        List<Long> ids = List.of(2L, 1L);
        Book book1 = createDummyBook(1L);
        Book book2 = createDummyBook(2L);

        given(bookRepository.findAllById(ids)).willReturn(List.of(book1, book2));
        given(fileRepository.findAllByJoinedIdInAndFileType(any(), eq(FileType.BOOK)))
                .willReturn(List.of());

        // when
        List<BookListResponse> results = bookService.getBooksByIds(ids);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).bookId()).isEqualTo(2L);
        assertThat(results.get(1).bookId()).isEqualTo(1L);
    }

    // --- Helper Method ---
    private Book createDummyBook(Long id) {
        Publisher publisher = new Publisher("Test Publisher");

        Book book = Book.builder()
                .isbn("978-000000000" + id)
                .bookName("Test Book")
                .bookDescription("Description")
                .bookPublicationDate(LocalDate.now())
                .bookIndex("Index")
                .bookPackaging(true)
                .bookState(BookState.ON_SALE)
                .bookStock(100)
                .bookRegularPrice(10000)
                .bookSalePrice(9000)
                .bookReviewRate(4.5)
                .publisher(publisher)
                .build();

        ReflectionTestUtils.setField(book, "bookId", id);

        if (book.getBookAuthors() == null) {
            ReflectionTestUtils.setField(book, "bookAuthors", new java.util.HashSet<>());
        }
        if (book.getBookCategories() == null) {
            ReflectionTestUtils.setField(book, "bookCategories", new java.util.HashSet<>());
        }
        if (book.getBookTags() == null) {
            ReflectionTestUtils.setField(book, "bookTags", new java.util.HashSet<>());
        }

        return book;
    }
}