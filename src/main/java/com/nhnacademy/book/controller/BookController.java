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

    @GetMapping
    public ResponseEntity<Page<BookListResponse>> getBooks(
            @PageableDefault(page = 0, size = 20, sort = "bookId", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooks(pageable));
    }

    @GetMapping("/{book-id}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable("book-id") Long bookId) {
        bookService.increaseViewCount(bookId);
        return ResponseEntity.ok(bookService.getBook(bookId));
    }

    @GetMapping(params = "bookOrders")
    public ResponseEntity<List<BookListResponse>> getBooksForOrder(@RequestParam List<Long> bookIds, @RequestParam List<Long> quantities) {
        return ResponseEntity.ok(bookService.getBooksByIds(bookIds));
    }

    @GetMapping("/best-sellers")
    public ResponseEntity<List<BookListResponse>> getBestSellers() {
        List<Long> bookIds = orderClient.getTopSellingBookIds(5);
        List<BookListResponse> bestSellers = bookService.getBooksByIds(bookIds);
        return ResponseEntity.ok(bestSellers);
    }

    @GetMapping("/popular-books")
    public ResponseEntity<List<BookListResponse>> getPopularBooks() {
        return ResponseEntity.ok(bookService.getPopularBooks());
    }

    // [★필수] 전체 신간 도서 Top 5 조회
    @GetMapping("/new-books")
    public ResponseEntity<List<BookListResponse>> getNewBooks() {
        return ResponseEntity.ok(bookService.getNewBooks());
    }

    // [★필수] 카테고리별 신간 Top 5 조회 (이게 있어야 404가 안 뜸)
    @GetMapping("/categories/{category-id}/top")
    public ResponseEntity<List<BookListResponse>> getBooksByCategory(@PathVariable("category-id") Long categoryId) {
        return ResponseEntity.ok(bookService.getBooksByCategory(categoryId));
    }

    @GetMapping("/categories/{category-id}")
    public ResponseEntity<Page<BookListResponse>> getBooksByCategoryPage(
            @PathVariable("category-id") Long categoryId,
            @PageableDefault(page = 0, size = 20, sort = "bookId", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooksByCategoryPage(categoryId, pageable));
    }

    @GetMapping("/categories/roots")
    public ResponseEntity<List<CategoryTreeResponse>> getRootCategories() {
        return ResponseEntity.ok(bookService.getRootCategories());
    }
}