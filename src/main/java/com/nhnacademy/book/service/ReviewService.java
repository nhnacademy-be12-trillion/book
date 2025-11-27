package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    Long createReview(ReviewCreateRequest request, Long memberId);

    Page<ReviewResponse> getReviewsByBookId(Long bookId, Pageable pageable);

    void updateReview(Long reviewId, ReviewUpdateRequest request, Long memberId);

    void updateBookAverageRating(Long bookId); // 평점 업데이트
}