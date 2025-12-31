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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 등록 성공 테스트 - 이미지 포함")
    void createReview_Success() throws Exception {
        // Given
        Long memberId = 1L;
        Long createdReviewId = 100L;

        // Record DTO 생성 (orderId, bookId, rate, content)
        ReviewCreateRequest requestDto = new ReviewCreateRequest(
                12345L, 10L, 5, "정말 좋은 책입니다!"
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // MockMultipartFile 설정
        // DTO를 보내는 'request' 파트의 Content-Type은 반드시 application/json이어야 함
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
                "images",
                "test.jpg",
                "image/jpeg",
                "image-content".getBytes()
        );

        given(reviewService.createReview(any(ReviewCreateRequest.class), anyList(), eq(memberId)))
                .willReturn(createdReviewId);

        // When & Then
        mockMvc.perform(multipart("/books/reviews")
                        .file(requestPart)
                        .file(imagePart)
                        .header("X-Member-Id", memberId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().string(String.valueOf(createdReviewId)))
                .andDo(print());

        verify(reviewService).createReview(any(ReviewCreateRequest.class), anyList(), eq(memberId));
    }

    @Test
    @DisplayName("리뷰 수정 성공 테스트")
    void updateReview_Success() throws Exception {
        // Given
        Long reviewId = 100L;
        Long memberId = 1L;

        // Record DTO 생성 (rate, content)
        ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(4, "내용 수정합니다.");

        // When & Then
        mockMvc.perform(put("/books/reviews/{review-id}", reviewId)
                        .header("X-Member-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andDo(print());

        verify(reviewService).updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(memberId));
    }

    @Test
    @DisplayName("특정 도서 리뷰 목록 조회 테스트 (페이징)")
    void getReviewsByBookId_Success() throws Exception {
        // Given
        Long bookId = 10L;
        Long memberId = 1L; // 추가된 필드값

        // Record DTO 직접 생성 (변경된 생성자 반영: memberId 추가)
        ReviewResponse mockResponse = new ReviewResponse(
                100L,           // reviewId
                memberId,       // memberId (추가됨)
                bookId,         // bookId
                "재미있는 자바",   // bookName
                5,              // reviewRate
                "최고입니다.",    // reviewContents
                LocalDateTime.now(), // createdAt
                LocalDateTime.now(),
                "작성자",        // writerName
                List.of("http://image.url/1.jpg") // imageUrls
        );

        Page<ReviewResponse> mockPage = new PageImpl<>(List.of(mockResponse));

        given(reviewService.getReviewsByBookId(eq(bookId), any(Pageable.class)))
                .willReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/books/{book-id}/reviews", bookId)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewId").value(100L))
                .andExpect(jsonPath("$.content[0].memberId").value(memberId)) // memberId 검증 추가
                .andExpect(jsonPath("$.content[0].bookName").value("재미있는 자바"))
                .andDo(print());

        verify(reviewService).getReviewsByBookId(eq(bookId), any(Pageable.class));
    }

    @Test
    @DisplayName("내 리뷰 목록 조회 테스트")
    void getMyReviews_Success() throws Exception {
        // Given
        Long memberId = 1L;
        Page<ReviewResponse> emptyPage = new PageImpl<>(Collections.emptyList());

        given(reviewService.getReviewsByMemberId(eq(memberId), any(Pageable.class)))
                .willReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/books/reviews/me")
                        .header("X-Member-Id", memberId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andDo(print());

        verify(reviewService).getReviewsByMemberId(eq(memberId), any(Pageable.class));
    }

    @Test
    @DisplayName("리뷰 작성 여부 확인 테스트 - True")
    void checkReviewExistence_Exists() throws Exception {
        // Given
        Long orderId = 12345L;
        given(reviewService.existsByOrderId(orderId)).willReturn(true);

        // When & Then
        mockMvc.perform(get("/books/reviews/check/{order-id}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(print());

        verify(reviewService).existsByOrderId(orderId);
    }
}