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
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 등록
     * [POST] /api/reviews
     * Content-Type: multipart/form-data
     *
     * @param request  : 리뷰 내용 (JSON) -> @RequestPart("request")
     * @param images   : 이미지 파일 리스트 (File) -> @RequestPart("images")
     * @param memberId : 헤더에서 추출한 회원 ID
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createReview(
            @RequestPart("request") ReviewCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        Long reviewId = reviewService.createReview(request, images, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewId);
    }

    /**
     * 특정 도서의 리뷰 목록 조회 (페이징)
     * [GET] /api/reviews/books/{bookId}?page=0&size=10
     */
    @GetMapping("/books/{bookId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByBookId(
            @PathVariable Long bookId,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewResponse> reviews = reviewService.getReviewsByBookId(bookId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 리뷰 수정
     * [PUT] /api/reviews/{reviewId}
     * Content-Type: application/json
     * (현재 로직상 이미지는 수정하지 않고 내용/평점만 수정함)
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequest request,
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        reviewService.updateReview(reviewId, request, memberId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/members/{memberId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByMember(
            @PathVariable Long memberId,
            @PageableDefault(size = 10, sort = "reviewId", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReviewResponse> responses = reviewService.getReviewsByMemberId(memberId, pageable);

        return ResponseEntity.ok(responses);
    }
}