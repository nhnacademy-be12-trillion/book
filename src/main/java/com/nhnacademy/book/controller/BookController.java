package com.nhnacademy.book.controller;

import com.nhnacademy.book.client.order.OrderClient;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.service.BookService;
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
    private final OrderClient orderClient;

    // 전체 도서 목록 조회 (페이징)
    // GET /api/books?page=0&size=20...
    @GetMapping
    public ResponseEntity<Page<BookListResponse>> getBooks(
            @PageableDefault(page = 0, size = 20, sort = "bookId", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooks(pageable));
    }

    // 도서 상세 조회 (조회수 증가 포함)
    // GET /api/books/{book-id}
    @GetMapping("/{book-id}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable("book-id") Long bookId) {
        bookService.increaseViewCount(bookId);
        return ResponseEntity.ok(bookService.getBook(bookId));
    }

    // 주문 대상 도서 목록 조회 (장바구니/주문서용)
    // GET /api/books?bookOrders&bookIds=1,2&quantities=1,2
    @GetMapping(params = "bookOrders")
    public ResponseEntity<List<BookListResponse>> getBooksForOrder(@RequestParam List<Long> bookIds, @RequestParam List<Long> quantities) {
        return ResponseEntity.ok(bookService.getBooksByIds(bookIds));
    }

    // 베스트셀러 조회 (판매량 기준 Top 5)
    // GET /api/books/best-sellers
    @GetMapping("/best-sellers")
    public ResponseEntity<List<BookListResponse>> getBestSellers() {
        List<Long> bookIds = orderClient.getTopSellingBookIds(5);
        List<BookListResponse> bestSellers = bookService.getBooksByIds(bookIds);
        return ResponseEntity.ok(bestSellers);
    }

    // 인기 도서 조회 (조회수 기준)
    // GET /api/books/popular-books
    @GetMapping("/popular-books")
    public ResponseEntity<List<BookListResponse>> getPopularBooks() {
        return ResponseEntity.ok(bookService.getPopularBooks());
    }

    // 전체 신간 도서 Top 5 조회
    // GET /api/books/new-books
    @GetMapping("/new-books")
    public ResponseEntity<List<BookListResponse>> getNewBooks() {
        return ResponseEntity.ok(bookService.getNewBooks());
    }

    // 카테고리별 신간 Top 5 조회
    // GET /api/books/categories/{category-id}/top
    @GetMapping("/categories/{category-id}/top")
    public ResponseEntity<List<BookListResponse>> getBooksByCategory(@PathVariable("category-id") Long categoryId) {
        return ResponseEntity.ok(bookService.getBooksByCategory(categoryId));
    }

    // 카테고리별 도서 리스트 조회 (페이징)
    // GET /api/books/categories/{category-id}?page=0...
    @GetMapping("/categories/{category-id}")
    public ResponseEntity<Page<BookListResponse>> getBooksByCategoryPage(
            @PathVariable("category-id") Long categoryId,
            @PageableDefault(page = 0, size = 20, sort = "bookId", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooksByCategoryPage(categoryId, pageable));
    }

    // 최상위 카테고리 목록 조회
    // GET /api/books/categories/roots
    @GetMapping("/categories/roots")
    public ResponseEntity<List<CategoryTreeResponse>> getRootCategories() {
        return ResponseEntity.ok(bookService.getRootCategories());
    }
}