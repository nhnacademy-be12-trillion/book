package com.nhnacademy.book.controller;

import com.nhnacademy.book.client.order.OrderClient;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.ReviewService;
import com.nhnacademy.book.service.impl.BookAiRegistrationService;
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
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ReviewService reviewService;
    private final OrderClient orderClient;

    // 도서 목록 조회 API (BookListResponse 반환)
    // GET /api/books?page=0&size=20
    @GetMapping
    public ResponseEntity<Page<BookListResponse>> getBooks(
            @PageableDefault(size = 20, sort = "bookId", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(bookService.getBooks(pageable));
    }

    // 도서 상세 조회 API (BookDetailResponse 반환)
    // GET /api/books/{bookId}
    @GetMapping("/{bookId}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable Long bookId) {
        // 조회수 증가 (DB 바로 안 가고 메모리에 쌓임)
        bookService.increaseViewCount(bookId);
        return ResponseEntity.ok(bookService.getBook(bookId));
    }


//    // 도서별 리뷰 목록 조회 API
//    // GET /api/books/{bookId}/reviews?page=0&size=5
//    @GetMapping("/{bookId}/reviews")
//    public ResponseEntity<Page<ReviewResponse>> getReviewsByBookId(
//            @PathVariable Long bookId,
//            Pageable pageable) {
//
//        return ResponseEntity.ok(reviewService.getReviewsByBookId(bookId, pageable));
//    }

    // 베스트셀러 도서 목록 조회 API
    @GetMapping("/best-sellers")
    public ResponseEntity<List<Long>> getBestSellers() {
        // TODO: 베스트셀러 도서 ID 받아서 List<Long> bookIds를 파라미터로 받아 해당 도서 정보 반환하는 기능 만들기
        // List<Long> bestSellerBookIds = orderClient.getTopSellingBookIds(5);

        return ResponseEntity.ok(orderClient.getTopSellingBookIds(5));
    }
    /**
     * 베스트셀러 Top 5 조회
     * GET /api/books/bestsellers
     */
    @GetMapping("/popularBooks")
    public ResponseEntity<List<BookListResponse>> getPopularBooks() {
        return ResponseEntity.ok(bookService.getPopularBooks());
    }

    /**
     * 카테고리별 신간 Top 5 조회
     * GET /api/books/categories/{categoryId}/top
     */
    @GetMapping("/categories/{categoryId}/top")
    public ResponseEntity<List<BookListResponse>> getBooksByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(bookService.getBooksByCategory(categoryId));
    }
}