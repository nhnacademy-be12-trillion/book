package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.service.BookIndexService;
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

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ReviewService reviewService;
    private final BookIndexService bookIndexService;
    private final BookAiRegistrationService bookRegistrationService;

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

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Long> createBook(
            @RequestPart("book") BookCreateRequest request,
            // "image" -> "file" 로 변경
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        // 변수명도 image -> file로 맞춤 (Service 메서드 파라미터 이름과 통일)
        Long bookId = bookService.createBook(request, file);
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

    //AI 도서등록 정보 가져오기
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookCreateRequest> getBookInfoByIsbn(@PathVariable String isbn) {
        BookCreateRequest response = bookRegistrationService.getBookInfoByIsbn(isbn);
        return ResponseEntity.ok(response);
    }

    // 주문 서비스가 호출할 API
    // 요청 예시: POST /api/books/1/deduct-stock  { "quantity": 2 }
    @PostMapping("/{bookId}/deduct-stock")
    public ResponseEntity<Void> deductStock(@PathVariable Long bookId, @RequestBody StockRequest request) {

        bookService.deductStock(bookId, request.quantity());
        return ResponseEntity.ok().build();
    }

    // DTO는 클래스 밑에 간단하게 record로 만들어도 됨
    public record StockRequest(int quantity) {}
}