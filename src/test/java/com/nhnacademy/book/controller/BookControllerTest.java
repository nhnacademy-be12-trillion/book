package com.nhnacademy.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.service.BookIndexService;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.ReviewService;
import com.nhnacademy.book.service.impl.BookAiRegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookController.class, properties = "spring.cloud.config.enabled=false")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private BookIndexService bookIndexService;

    @MockitoBean
    private BookAiRegistrationService bookAiRegistrationService;

    @Test
    @DisplayName("도서 목록 조회 (GET /api/books)")
    void getBooks() throws Exception {
        // given
        BookListResponse response = new BookListResponse(
                "테스트 책", "출판사", true, BookState.ON_SALE,
                100, 10000, 9000, 4.5, "이미지"
        );
        Page<BookListResponse> page = new PageImpl<>(List.of(response));

        given(bookService.getBooks(any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/books")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bookName").value("테스트 책"))
                .andDo(print());
    }

    @Test
    @DisplayName("도서 상세 조회 (GET /api/books/{bookId})")
    void getBook() throws Exception {
        // given
        Long bookId = 1L;

        BookDetailResponse response = new BookDetailResponse(
                "상세 책",             // bookName
                "설명",                // bookDescription
                "출판사",              // bookPublisher
                LocalDate.now(),       // bookPublicationDate
                "목차",                // bookIndex
                true,                  // bookPackaging
                BookState.ON_SALE,     // bookState
                100,                   // bookStock
                10000,                 // bookRegularPrice
                9000,                  // bookSalePrice
                4.5,                   // bookReviewRate
                "img.jpg",             // bookImage
                0                      // viewCount
        );

        given(bookService.getBook(bookId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/books/{bookId}", bookId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("상세 책"))
                .andDo(print());

        // 조회수 증가 로직 호출 검증
        verify(bookService).increaseViewCount(bookId);
    }

    @Test
    @DisplayName("도서 등록 (POST /api/books) - MultipartFile 전송 테스트")
    void createBook() throws Exception {
        // given
        BookCreateRequest request = new BookCreateRequest(
                "978-1234", "새 책", "설명", "출판사", LocalDate.now(), "목차",
                true, BookState.ON_SALE, 100, 10000, 9000, "img.jpg"
        );

        // 1. JSON 데이터를 파일 파트(Part)로 변환 (@RequestPart("book") 대응)
        String requestJson = objectMapper.writeValueAsString(request);
        MockMultipartFile bookPart = new MockMultipartFile(
                "book", // 파라미터 이름 (Controller의 @RequestPart("book")과 일치해야 함)
                "",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // 2. 실제 이미지 파일 파트 (@RequestPart("file") 대응)
        MockMultipartFile filePart = new MockMultipartFile(
                "file", // 파라미터 이름
                "test.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        // 3. Service Mocking: 인자가 (Request, File) 두 개여야 함
        given(bookService.createBook(any(BookCreateRequest.class), any(MultipartFile.class))).willReturn(100L);

        // when & then
        mockMvc.perform(multipart("/api/books")
                        .file(bookPart) // JSON 데이터
                        .file(filePart) // 이미지 파일
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()) // 201 Created
                .andExpect(content().string("100"))
                .andDo(print());
    }

    @Test
    @DisplayName("도서 수정 (PUT /api/books/{bookId})")
    void updateBook() throws Exception {
        // given
        Long bookId = 1L;
        BookUpdateRequest request = new BookUpdateRequest(
                "수정 책", "수정 설명", "수정 목차", true,
                BookState.SOLD_OUT, 50, 10.0, "new_img.jpg"
        );

        // when & then
        mockMvc.perform(put("/api/books/{bookId}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(print());

        verify(bookService).updateBook(eq(bookId), any(BookUpdateRequest.class));
    }

    @Test
    @DisplayName("도서 삭제 (DELETE /api/books/{bookId})")
    void deleteBook() throws Exception {
        // given
        Long bookId = 1L;

        // when & then
        mockMvc.perform(delete("/api/books/{bookId}", bookId))
                .andExpect(status().isOk())
                .andDo(print());

        verify(bookService).deleteBook(bookId);
    }

    @Test
    @DisplayName("목차 증강 배치 (POST /api/books/toc/augment)")
    void augmentBookIndices() throws Exception {
        // given
        given(bookIndexService.augmentBookIndexBatch(any(Pageable.class))).willReturn(50);

        // when & then
        mockMvc.perform(post("/api/books/toc/augment")
                        .param("size", "800"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("50권")))
                .andDo(print());
    }

    @Test
    @DisplayName("목차 단건 조회 (GET /api/books/toc/{isbn}) - 성공")
    void getBookToc_Found() throws Exception {
        // given
        String isbn = "9781234567890";
        String toc = "1. 서론 2. 본론";
        given(bookIndexService.getTableOfContentsByIsbn(isbn)).willReturn(toc);

        // when & then
        mockMvc.perform(get("/api/books/toc/{isbn}", isbn))
                .andExpect(status().isOk())
                .andExpect(content().string(toc))
                .andDo(print());
    }

    @Test
    @DisplayName("목차 단건 조회 (GET /api/books/toc/{isbn}) - 실패(404)")
    void getBookToc_NotFound() throws Exception {
        // given
        String isbn = "9780000000000";
        given(bookIndexService.getTableOfContentsByIsbn(isbn)).willReturn(null);

        // when & then
        mockMvc.perform(get("/api/books/toc/{isbn}", isbn))
                .andExpect(status().isNotFound()) // 404 check
                .andDo(print());
    }

    @Test
    @DisplayName("도서별 리뷰 목록 조회 (GET /api/books/{bookId}/reviews)")
    void getReviewsByBookId() throws Exception {
        // given
        Long bookId = 1L;
        ReviewResponse review = new ReviewResponse(1L, 5, "좋아요", LocalDateTime.now(), "작성자");
        Page<ReviewResponse> page = new PageImpl<>(List.of(review));

        given(reviewService.getReviewsByBookId(eq(bookId), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/books/{bookId}/reviews", bookId)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewContents").value("좋아요"))
                .andDo(print());
    }

    @Test
    @DisplayName("AI 도서정보 가져오기 (GET /api/books/isbn/{isbn})")
    void getBookInfoByIsbn() throws Exception {
        // given
        String isbn = "9781234";
        BookCreateRequest request = new BookCreateRequest(
                isbn, "AI 책", "AI 설명", "출판사", LocalDate.now(), "목차",
                true, BookState.ON_SALE, 100, 20000, 18000, "ai.jpg"
        );

        given(bookAiRegistrationService.getBookInfoByIsbn(isbn)).willReturn(request);

        // when & then
        mockMvc.perform(get("/api/books/isbn/{isbn}", isbn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("AI 책"))
                .andDo(print());
    }

    @Test
    @DisplayName("재고 차감 (POST /api/books/{bookId}/deduct-stock)")
    void deductStock() throws Exception {
        // given
        Long bookId = 1L;
        BookController.StockRequest stockRequest = new BookController.StockRequest(2);

        // when & then
        mockMvc.perform(post("/api/books/{bookId}/deduct-stock", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockRequest)))
                .andExpect(status().isOk())
                .andDo(print());

        verify(bookService).deductStock(bookId, 2);
    }
}