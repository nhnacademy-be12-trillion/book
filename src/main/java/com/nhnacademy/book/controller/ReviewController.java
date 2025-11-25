package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성
    @PostMapping
    public ResponseEntity<Long> createReview(
            @RequestBody ReviewCreateRequest request,
            @RequestHeader("X-USER-ID") Long memberId // Gateway에서 인증된 ID 수신
    ) {
        // 받은 ID와 DTO를 Service로 위임
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request, memberId));
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequest request,
            @RequestHeader("X-USER-ID") Long memberId // Gateway에서 인증된 ID 수신
    ) {
        // Service 내부에서 권한 확인 및 수정 로직 실행
        reviewService.updateReview(reviewId, request, memberId);
        return ResponseEntity.ok().build();
    }

    //  리뷰 목록 조회
    @GetMapping("/books/{bookId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByBookId(
            @PathVariable Long bookId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByBookId(bookId, pageable));
    }
}