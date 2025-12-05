package com.nhnacademy.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import com.nhnacademy.book.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewController.class, properties = "spring.cloud.config.enabled=false")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 작성 (POST /api/reviews)")
    void createReview() throws Exception {
        // given
        Long memberId = 1L;
        Long bookId = 10L;
        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 5, "정말 좋은 책입니다!");

        // 서비스가 생성된 리뷰 ID (예: 100L)를 반환한다고 가정
        given(reviewService.createReview(any(ReviewCreateRequest.class), eq(memberId)))
                .willReturn(100L);

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .header("X-USER-ID", memberId) // Gateway 인증 헤더 시뮬레이션
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // 201 Created
                .andExpect(content().string("100")) // Body에 ID 반환 확인
                .andDo(print());

        // 서비스 호출 검증
        verify(reviewService).createReview(any(ReviewCreateRequest.class), eq(memberId));
    }

    @Test
    @DisplayName("리뷰 수정 (PUT /api/reviews/{reviewId})")
    void updateReview() throws Exception {
        // given
        Long reviewId = 100L;
        Long memberId = 1L;
        ReviewUpdateRequest request = new ReviewUpdateRequest(3, "내용 수정합니다.");

        // void 메서드이므로 willDoNothing 사용 (또는 생략 가능)
        willDoNothing().given(reviewService).updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(memberId));

        // when & then
        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                        .header("X-USER-ID", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // 200 OK
                .andDo(print());

        // 서비스 호출 검증
        verify(reviewService).updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(memberId));
    }

    @Test
    @DisplayName("도서별 리뷰 목록 조회 (GET /api/reviews/books/{bookId})")
    void getReviewsByBookId() throws Exception {
        // given
        Long bookId = 10L;
        ReviewResponse reviewResponse = new ReviewResponse(
                100L, 5, "최고예요", LocalDateTime.now(), "작성자"
        );
        Page<ReviewResponse> page = new PageImpl<>(List.of(reviewResponse));

        given(reviewService.getReviewsByBookId(eq(bookId), any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/reviews/books/{bookId}", bookId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewContents").value("최고예요"))
                .andExpect(jsonPath("$.content[0].writerName").value("작성자"))
                .andDo(print());

        verify(reviewService).getReviewsByBookId(eq(bookId), any(Pageable.class));
    }
}