//package com.nhnacademy.book.service.impl;
//
//import com.nhnacademy.book.dto.book.BookCreateRequest;
//import com.nhnacademy.book.dto.book.BookDetailResponse;
//import com.nhnacademy.book.dto.book.BookListResponse;
//import com.nhnacademy.book.dto.book.BookUpdateRequest;
//import com.nhnacademy.book.entity.Book;
//import com.nhnacademy.book.entity.BookState;
//import com.nhnacademy.book.entity.Publisher;
//import com.nhnacademy.book.repository.BookRepository;
//import com.nhnacademy.book.repository.PublisherRepository;
//import com.nhnacademy.book.repository.AuthorRepository;
//import com.nhnacademy.book.service.FileService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class BookServiceImplTest {
//
//    @InjectMocks
//    private BookServiceImpl bookService;
//
//    @Mock
//    private BookRepository bookRepository;
//
//    @Mock
//    private FileService fileService;
//
//    @Mock
//    private MinioService minioService;
//
//    // ServiceImpl에 새로 추가된 Repository Mocking 필요 (NullPointerException 방지용)
//    @Mock
//    private PublisherRepository publisherRepository;
//
//    @Mock
//    private AuthorRepository authorRepository;
//
//    // --- Create Tests ---
//
//    @Test
//    @DisplayName("도서 등록 성공 - 파일 없이 요청 시 기본 로직 수행")
//    void createBook_Success() {
//        // given
//        BookCreateRequest request = new BookCreateRequest(
//                "978-1-2345-6789-0",
//                "테스트 도서",
//                "설명",
//                "테스트 출판사",
//                "테스트 작가", // [수정] 누락된 bookAuthor 추가
//                LocalDate.now(),
//                "목차",
//                true,
//                BookState.ON_SALE,
//                100, // stock
//                20000, // regular price
//                18000, // sale price
//                "image_url_from_aladin"
//        );
//
//        Book savedBook = new Book();
//        savedBook.setBookId(1L);
//        savedBook.setBookName("테스트 도서"); // 검증을 위해 이름 설정
//
//        // Mocking
//        given(bookRepository.save(any(Book.class))).willReturn(savedBook);
//        given(minioService.uploadFromUrl(anyString())).willReturn("uploaded_minio_url");
//
//        // when
//        Long bookId = bookService.createBook(request, null);
//
//        // then
//        assertThat(bookId).isEqualTo(1L);
//        verify(bookRepository).save(any(Book.class));
//        verify(minioService).uploadFromUrl(request.bookImage());
//        verify(fileService).saveBookImage(eq(1L), eq("uploaded_minio_url"));
//    }
//
//    @Test
//    @DisplayName("도서 등록 성공 - MultipartFile 포함 시 Minio 업로드 수행")
//    void createBook_WithFile_Success() {
//        // given
//        BookCreateRequest request = new BookCreateRequest(
//                "978-1-2345-6789-0",
//                "책",
//                "설명",
//                "출판사",
//                "작가", // [수정] 누락된 bookAuthor 추가
//                LocalDate.now(),
//                "목차",
//                true,
//                BookState.ON_SALE,
//                10,
//                10000,
//                9000,
//                null
//        );
//        MultipartFile mockFile = mock(MultipartFile.class);
//        given(mockFile.isEmpty()).willReturn(false);
//
//        Book savedBook = new Book();
//        savedBook.setBookId(2L);
//
//        given(minioService.uploadImage(mockFile)).willReturn("minio_image_url");
//        given(bookRepository.save(any(Book.class))).willReturn(savedBook);
//
//        // when
//        Long bookId = bookService.createBook(request, mockFile);
//
//        // then
//        assertThat(bookId).isEqualTo(2L);
//        verify(minioService).uploadImage(mockFile);
//        verify(fileService).saveBookImage(2L, "minio_image_url");
//    }
//
//
//    // --- Read Tests (Get) ---
//
//    @Test
//    @DisplayName("도서 목록 조회 성공")
//    void getBooks_Success() {
//        // given
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Publisher publisher = new Publisher("테스트 출판사");
//        Book book = new Book();
//        book.setBookId(1L);
//        book.setBookName("테스트 책");
//        book.setPublisher(publisher);
//
//        Page<Book> bookPage = new PageImpl<>(List.of(book));
//
//        given(bookRepository.findAll(pageable)).willReturn(bookPage);
//
//        // when
//        Page<BookListResponse> result = bookService.getBooks(pageable);
//
//        // then
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).bookName()).isEqualTo("테스트 책");
//    }
//
//    @Test
//    @DisplayName("도서 상세 조회 성공")
//    void getBook_Success() {
//        // given
//        Long bookId = 1L;
//
//        Publisher publisher = new Publisher("테스트 출판사");
//        Book book = new Book();
//        book.setBookId(bookId);
//        book.setBookName("상세 조회 책");
//        book.setPublisher(publisher);
//
//        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
//
//        // when
//        BookDetailResponse response = bookService.getBook(bookId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.bookName()).isEqualTo("상세 조회 책");
//        assertThat(response.bookPublisher()).isEqualTo("테스트 출판사");
//    }
//
//    @Test
//    @DisplayName("도서 상세 조회 실패 - 존재하지 않는 ID")
//    void getBook_NotFound() {
//        // given
//        Long bookId = 999L;
//        given(bookRepository.findById(bookId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> bookService.getBook(bookId))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("해당 도서가 존재하지 않습니다");
//    }
//
//
//    // --- Update Tests ---
//
//    @Test
//    @DisplayName("도서 수정 성공 - 할인율에 따른 판매가 자동 계산 확인")
//    void updateBook_Success() {
//        // given
//        Long bookId = 1L;
//
//        // 기존 도서 (정가 20,000원)
//        Book existingBook = new Book();
//        existingBook.setBookId(bookId);
//        existingBook.setBookRegularPrice(20000);
//        existingBook.setBookSalePrice(18000); // 기존 판매가
//
//        given(bookRepository.findById(bookId)).willReturn(Optional.of(existingBook));
//
//        // 수정 요청 (할인율 10% 적용 요청)
//        BookUpdateRequest request = new BookUpdateRequest(
//                "수정된 제목",
//                "수정된 설명",
//                "수정된 목차",
//                false, // packaging
//                BookState.SOLD_OUT,
//                50, // stock
//                10.0, // discountRate (10%)
//                "new_image.jpg"
//        );
//
//        // when
//        bookService.updateBook(bookId, request);
//
//        // then
//        // 1. 필드 업데이트 확인
//        assertThat(existingBook.getBookName()).isEqualTo("수정된 제목");
//        assertThat(existingBook.getBookState()).isEqualTo(BookState.SOLD_OUT);
//
//        // 2. 가격 계산 로직 확인: 20000 * (1 - 10/100) = 18000
//        assertThat(existingBook.getBookSalePrice()).isEqualTo(18000);
//    }
//
//    @Test
//    @DisplayName("도서 수정 실패 - 존재하지 않는 도서")
//    void updateBook_NotFound() {
//        // given
//        Long bookId = 999L;
//        BookUpdateRequest request = mock(BookUpdateRequest.class);
//        given(bookRepository.findById(bookId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> bookService.updateBook(bookId, request))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("수정할 도서가 없습니다");
//    }
//
//
//    // --- Delete Tests (Soft Delete) ---
//
//    @Test
//    @DisplayName("도서 삭제 성공 - 상태가 SALE_END로 변경됨")
//    void deleteBook_Success() {
//        // given
//        Long bookId = 1L;
//        Book book = new Book();
//        book.setBookId(bookId);
//        book.setBookState(BookState.ON_SALE);
//
//        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
//
//        // when
//        bookService.deleteBook(bookId);
//
//        // then
//        assertThat(book.getBookState()).isEqualTo(BookState.SALE_END);
//    }
//
//
//    // --- Other Logic Tests ---
//
//    @Test
//    @DisplayName("조회수 증가 성공")
//    void increaseViewCount_Success() {
//        // given
//        Long bookId = 1L;
//
//        // when
//        bookService.increaseViewCount(bookId);
//
//        // then
//        verify(bookRepository).updateViewCount(bookId);
//    }
//
//    @Test
//    @DisplayName("재고 차감 성공")
//    void deductStock_Success() {
//        // given
//        Long bookId = 1L;
//        Book book = new Book();
//        book.setBookStock(10);
//        book.setBookState(BookState.ON_SALE);
//
//        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
//
//        // when
//        bookService.deductStock(bookId, 3);
//
//        // then
//        assertThat(book.getBookStock()).isEqualTo(7);
//        assertThat(book.getBookState()).isEqualTo(BookState.ON_SALE);
//    }
//
//    @Test
//    @DisplayName("재고 차감 - 재고가 0이 되면 품절 상태로 변경")
//    void deductStock_SoldOut() {
//        // given
//        Long bookId = 1L;
//        Book book = new Book();
//        book.setBookStock(5);
//        book.setBookState(BookState.ON_SALE);
//
//        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
//
//        // when
//        bookService.deductStock(bookId, 5); // 전부 구매
//
//        // then
//        assertThat(book.getBookStock()).isEqualTo(0);
//        assertThat(book.getBookState()).isEqualTo(BookState.SOLD_OUT);
//    }
//
//    @Test
//    @DisplayName("재고 차감 실패 - 재고 부족 예외 발생")
//    void deductStock_NotEnough() {
//        // given
//        Long bookId = 1L;
//        Book book = new Book();
//        book.setBookStock(2); // 재고 2개
//
//        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
//
//        // when & then
//        assertThatThrownBy(() -> bookService.deductStock(bookId, 3))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("재고가 부족합니다");
//    }
//}