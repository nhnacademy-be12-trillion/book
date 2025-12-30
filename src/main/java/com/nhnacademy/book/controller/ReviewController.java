package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import com.nhnacademy.book.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books")
public class ReviewController {

    private final ReviewService reviewService;

     // 리뷰 등록 API
     // POST /api/books/reviews
    @PostMapping(value = "/reviews",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE,MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Long> createReview(
            @RequestPart("request") ReviewCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestHeader("X-Member-Id") Long memberId // Gateway가 전달하는 인증 헤더
    ) {
        Long reviewId = reviewService.createReview(request, images, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewId);
    }

     // 리뷰 수정 API
     // PUT /api/books/reviews/{review-id}
    @PutMapping("/reviews/{review-id}")
    public ResponseEntity<Void> updateReview(
            @PathVariable("review-id") Long reviewId,
            @RequestBody ReviewUpdateRequest request,
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        reviewService.updateReview(reviewId, request, memberId);
        return ResponseEntity.ok().build();
    }


     // 특정 도서의 리뷰 목록 조회 API
     // GET /api/books/{book-id}/reviews
    @GetMapping("/{book-id}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByBookId(
            @PathVariable("book-id") Long bookId,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewResponse> reviews = reviewService.getReviewsByBookId(bookId, pageable);
        return ResponseEntity.ok(reviews);
    }

     // 마이페이지 - 내 전체 리뷰 목록 조회 API
     // GET /api/books/reviews/me
    @GetMapping("/reviews/me")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @RequestHeader("X-Member-Id") Long memberId,
            @PageableDefault(size = 10, sort = "reviewId", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReviewResponse> responses = reviewService.getReviewsByMemberId(memberId, pageable);
        return ResponseEntity.ok(responses);
    }

    // 리뷰 작성 여부 확인 API
    // GET /api/books/reviews/check/{order-id}
    @GetMapping("/reviews/check/{order-id}")
    public ResponseEntity<Boolean>   checkReviewExistence(@PathVariable("order-id") Long orderId) {
        boolean exists = reviewService.existsByOrderId(orderId);
        return ResponseEntity.ok(exists);
    }
}