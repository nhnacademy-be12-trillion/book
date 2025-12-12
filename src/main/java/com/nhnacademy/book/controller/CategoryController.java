package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getAllCategories() {
        List<CategoryTreeResponse> responses = categoryService.getCategoryTree();
        return ResponseEntity.ok(responses);
    }

}