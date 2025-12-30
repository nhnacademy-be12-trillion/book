package com.nhnacademy.book.controller;

import com.nhnacademy.book.client.order.OrderClient;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private OrderClient orderClient;

    // 테스트용 더미 BookListResponse 생성
    private BookListResponse createBookListResponse(Long id) {
        return new BookListResponse(
                id,
                "테스트 책 " + id,
                "테스트 작가",
                "테스트 출판사",
                BookState.ON_SALE,
                20000,
                18000,
                10,
                4.5,
                "http://image.url/test.jpg",
                List.of("태그1", "태그2")
        );
    }

    @Test
    @DisplayName("전체 도서 목록 조회 (페이징)")
    void getBooks() throws Exception {
        // given
        BookListResponse book = createBookListResponse(1L);
        Page<BookListResponse> mockPage = new PageImpl<>(List.of(book));

        given(bookService.getBooks(any(PageRequest.class))).willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/books")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bookName").value("테스트 책 1"))
                .andDo(print());
    }

    @Test
    @DisplayName("도서 상세 조회 - 성공 및 조회수 증가 확인")
    void getBook() throws Exception {
        // given
        Long bookId = 1L;
        BookDetailResponse mockResponse = new BookDetailResponse(
                bookId,
                "978-1234567890",
                "상세 페이지 책",
                "작가",
                "설명입니다",
                "출판사",
                LocalDate.now(),
                "목차",
                true,
                BookState.ON_SALE,
                100,
                20000,
                18000,
                10,
                4.8,
                "image.jpg",
                50,
                List.of(new BookDetailResponse.CategoryInfo(10L, "국내도서"))
        );

        given(bookService.getBook(bookId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/books/{book-id}", bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("상세 페이지 책"))
                .andExpect(jsonPath("$.categoryPath[0].categoryName").value("국내도서"))
                .andDo(print());

        // 조회수 증가 로직 호출 여부 검증
        verify(bookService).increaseViewCount(bookId);
    }

    @Test
    @DisplayName("주문 대상 도서 목록 조회")
    void getBooksForOrder() throws Exception {
        // given
        List<Long> bookIds = List.of(1L, 2L);
        List<BookListResponse> responses = List.of(createBookListResponse(1L), createBookListResponse(2L));

        given(bookService.getBooksByIds(bookIds)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/books")
                        .param("bookOrders", "")
                        .param("bookIds", "1,2")
                        .param("quantities", "1,1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andDo(print());
    }

    @Test
    @DisplayName("베스트셀러 조회 - OrderClient 연동 확인")
    void getBestSellers() throws Exception {
        // given
        List<Long> topIds = List.of(10L, 20L);
        List<BookListResponse> responses = List.of(createBookListResponse(10L), createBookListResponse(20L));

        given(orderClient.getTopSellingBookIds(5)).willReturn(topIds);
        given(bookService.getBooksByIds(topIds)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/books/best-sellers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andDo(print());

        // verify
        verify(orderClient).getTopSellingBookIds(5);
    }

    @Test
    @DisplayName("인기 도서 조회")
    void getPopularBooks() throws Exception {
        // given
        given(bookService.getPopularBooks()).willReturn(List.of(createBookListResponse(1L)));

        // when & then
        mockMvc.perform(get("/books/popular-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookName").exists());
    }

    @Test
    @DisplayName("신간 도서 조회")
    void getNewBooks() throws Exception {
        // given
        given(bookService.getNewBooks()).willReturn(List.of(createBookListResponse(5L)));

        // when & then
        mockMvc.perform(get("/books/new-books"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("카테고리별 신간 Top 5 조회")
    void getBooksByCategoryTop() throws Exception {
        // given
        Long categoryId = 100L;
        given(bookService.getBooksByCategory(categoryId)).willReturn(List.of(createBookListResponse(1L)));

        // when & then
        mockMvc.perform(get("/books/categories/{category-id}/top", categoryId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("카테고리별 도서 리스트 조회 (페이징)")
    void getBooksByCategoryPage() throws Exception {
        // given
        Long categoryId = 100L;
        Page<BookListResponse> page = new PageImpl<>(List.of(createBookListResponse(1L)));

        given(bookService.getBooksByCategoryPage(eq(categoryId), any(PageRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/books/categories/{category-id}", categoryId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bookId").value(1L));
    }

    @Test
    @DisplayName("최상위 카테고리 목록 조회")
    void getRootCategories() throws Exception {
        // given
        CategoryTreeResponse rootCat = new CategoryTreeResponse(1L, "국내도서", List.of());
        given(bookService.getRootCategories()).willReturn(List.of(rootCat));

        // when & then
        mockMvc.perform(get("/books/categories/roots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("국내도서"));
    }
}