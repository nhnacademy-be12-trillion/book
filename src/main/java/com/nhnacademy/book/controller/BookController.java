package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.BookCreateRequest;
import com.nhnacademy.book.dto.BookDetailResponse;
import com.nhnacademy.book.dto.BookListResponse;
import com.nhnacademy.book.dto.BookUpdateRequest;
import com.nhnacademy.book.service.BookService;
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

    // 1. 도서 목록 조회 API (BookListResponse 반환)
    // GET /api/books?page=0&size=20
    @GetMapping
    public ResponseEntity<Page<BookListResponse>> getBooks(
            @PageableDefault(size = 20, sort = "bookId", direction = Sort.Direction.DESC) Pageable pageable) {

        // Service는 List DTO를 반환하며, 가벼운 데이터만 전송됩니다.
        return ResponseEntity.ok(bookService.getBooks(pageable));
    }

    // 2. 도서 상세 조회 API (BookDetailResponse 반환)
    // GET /api/books/{bookId}
    @GetMapping("/{bookId}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable Long bookId) {

        // Service는 Detail DTO를 반환하며, 모든 상세 정보를 포함합니다.
        return ResponseEntity.ok(bookService.getBook(bookId));
    }

    // 3. 도서 등록 API (BookCreateRequest 요청)
    // POST /api/books
    @PostMapping
    public ResponseEntity<Long> createBook(@RequestBody BookCreateRequest request) {

        Long bookId = bookService.createBook(request);

        // 등록 성공 시 201 Created와 함께 생성된 ID를 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(bookId);
    }

    // 4. 도서 수정 API
    @PutMapping("/{bookId}")
    public ResponseEntity<Void> updateBook(@PathVariable Long bookId, @RequestBody BookUpdateRequest request) {
        bookService.updateBook(bookId, request);
        return ResponseEntity.ok().build();
    }

    // 5. 도서 삭제 API
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok().build();
    }
}