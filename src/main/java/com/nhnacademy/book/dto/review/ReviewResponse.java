package com.nhnacademy.book.dto.review;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.Review;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse (
     Long reviewId,
     int reviewRate,
     String reviewContents,
     LocalDateTime createdAt,
     String writerName,
     List<String> imageUrls
) {
    public static ReviewResponse from(Review review, List<String>imageUrls) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getReviewRate(),
                review.getReviewContents(),
                review.getCreatedAt(),
                review.getMember().getName(),
                imageUrls
        );
    }
}