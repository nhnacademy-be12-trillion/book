package com.nhnacademy.book.controller;

import com.nhnacademy.book.controller.docs.CategoryControllerDocs;
import com.nhnacademy.book.dto.category.BookCategoryResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/books/categories")
@RequiredArgsConstructor
public class CategoryController implements CategoryControllerDocs {

    private final CategoryService categoryService;

    // 카테고리 전체 트리 구조 조회 API
    // GET /api/books/categories
    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getAllCategories() {
        List<CategoryTreeResponse> responses = categoryService.getCategoryTree();
        return ResponseEntity.ok(responses);
    }

    // 특정 도서들의 카테고리 정보 조회 API
    // GET /api/books/categories?bookIds={bookIds}
    @GetMapping(params = "bookIds")
    public List<BookCategoryResponse> getBookCategories(@RequestParam List<Long> bookIds) {
        return BookCategoryResponse.of(categoryService.getCategoryIds(bookIds));
    }
}