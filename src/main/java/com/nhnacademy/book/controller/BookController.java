package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.service.BookIndexService;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ReviewService reviewService;
    private final BookIndexService bookIndexService;

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

        return ResponseEntity.ok(bookService.getBook(bookId));
    }

    // 도서 등록 API (BookCreateRequest 요청)
    // POST /api/books
    @PostMapping
    public ResponseEntity<Long> createBook(@RequestBody BookCreateRequest request) {

        Long bookId = bookService.createBook(request);

        // 등록 성공 시 201 Created와 함께 생성된 ID 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(bookId);
    }

    // 도서 수정 API
    @PutMapping("/{bookId}")
    public ResponseEntity<Void> updateBook(@PathVariable Long bookId, @RequestBody BookUpdateRequest request) {
        bookService.updateBook(bookId, request);
        return ResponseEntity.ok().build();
    }

    // 도서 삭제 API
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/toc/augment")
    public ResponseEntity<String> augmentBookIndices(
            @PageableDefault(size = 800) Pageable pageable) {

        int count = bookIndexService.augmentBookIndexBatch(pageable);

        return ResponseEntity.ok("목차 증강 작업 완료. 총 " + count + "권의 도서를 처리했습니다.");
    }

    @GetMapping("/toc/{isbn}")
    public ResponseEntity<String> getBookToc(@PathVariable String isbn) {

        // 새로 만든 단일 조회 메서드 사용
        String toc = bookIndexService.getTableOfContentsByIsbn(isbn);

        if (toc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toc);
    }

    // 도서별 리뷰 목록 조회 API
    // GET /api/books/{bookId}/reviews?page=0&size=5
    @GetMapping("/{bookId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByBookId(
            @PathVariable Long bookId,
            Pageable pageable) {

        return ResponseEntity.ok(reviewService.getReviewsByBookId(bookId, pageable));
    }



}