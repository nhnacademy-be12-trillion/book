package com.nhnacademy.book.dto.review;

import com.nhnacademy.book.entity.Review;
import java.time.LocalDateTime;

public record ReviewResponse (
     Long reviewId,
     int reviewRate,
     String reviewContents,
     LocalDateTime createdAt,
     String writerName
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getReviewRate(),
                review.getReviewContents(),
                review.getCreatedAt(),
                review.getMember().getName()
        );
    }
}