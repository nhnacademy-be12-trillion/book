package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReviewService {

    Long createReview(ReviewCreateRequest request, List<MultipartFile> images, Long memberId);

    Page<ReviewResponse> getReviewsByBookId(Long bookId, Pageable pageable);

    void updateReview(Long reviewId, ReviewUpdateRequest request, Long memberId);

    void updateBookAverageRating(Long bookId); // 평점 업데이트

    Page<ReviewResponse> getReviewsByMemberId(Long memberId, Pageable pageable);

    // 주문 ID로 리뷰 존재 여부 확인
    boolean existsByOrderId(Long orderId);
}