package com.nhnacademy.book.controller;

import com.nhnacademy.book.client.order.OrderClient;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ReviewService reviewService;
    private final OrderClient orderClient;

    // 도서 목록 조회 API
    // GET /api/books?page=0&size=20
    @GetMapping
    public ResponseEntity<Page<BookListResponse>> getBooks(
            @PageableDefault(page = 0, size = 20, sort = "bookId", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(bookService.getBooks(pageable));
    }

    // 도서 상세 조회 API (BookDetailResponse 반환)
    // GET /api/books/{book-id}
    @GetMapping("/{book-id}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable("book-id") Long bookId) {
        // 조회수 증가 (DB 바로 안 가고 메모리에 쌓임)
        bookService.increaseViewCount(bookId);
        return ResponseEntity.ok(bookService.getBook(bookId));
    }

    // 베스트셀러 도서 목록 조회 API
    // GET /api/books//best-sellers
    @GetMapping("/best-sellers")
    public ResponseEntity<List<BookListResponse>> getBestSellers() {
        // 베스트셀러 도서 ID 받아서 List<Long> bookIds를 파라미터로 받아 해당 도서 정보 반환하는 기능
         List<Long> bookIds = orderClient.getTopSellingBookIds(5);
         List<BookListResponse> bestSellers = bookService.getBooksByIds(bookIds);
        return ResponseEntity.ok(bestSellers);
    }
    // 조회 수 많은 책 Top 5 조회 API
    // GET /api/books/popular-books
    @GetMapping("/popular-books")
    public ResponseEntity<List<BookListResponse>> getPopularBooks() {
        return ResponseEntity.ok(bookService.getPopularBooks());
    }

     // 카테고리별 신간 Top 5 조회 API
     // GET /api/books/categories/{category-id}/top
    @GetMapping("/categories/{category-id}/top")
    public ResponseEntity<List<BookListResponse>> getBooksByCategory( @PathVariable("category-id") Long categoryId) {
        return ResponseEntity.ok(bookService.getBooksByCategory(categoryId));
    }
}