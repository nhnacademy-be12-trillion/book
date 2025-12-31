package com.nhnacademy.book.dto.review;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Review;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse (
        Long reviewId,
        Long memberId,
        Long bookId,
        String bookName,
        int reviewRate,
        String reviewContents,
        LocalDateTime createdAt,
        String writerName,
        List<String> imageUrls
) {
    public static ReviewResponse from(Review review, List<String> imageUrls) {
        Book book = review.getBook();
        return new ReviewResponse(
                review.getReviewId(),
                review.getMemberId(), // [추가] 엔티티에서 memberId 추출
                book.getBookId(),
                book.getBookName(),
                review.getReviewRate(),
                review.getReviewContents(),
                review.getCreatedAt(),
                "작성자", // 기본값 (컨트롤러에서 실제 이름으로 덮어씌움)
                imageUrls
        );
    }

    public ReviewResponse withWriterName(String name) {
        return new ReviewResponse(
                this.reviewId, this.memberId, this.bookId, this.bookName,
                this.reviewRate, this.reviewContents,
                this.createdAt, name, this.imageUrls
        );
    }
}