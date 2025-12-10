package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.category.CategorySearchResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.repository.CategoryRepository;
import com.nhnacademy.book.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository; //이거 조만간 없애고 카테고리 서비스만 주입바도록 병합 예정
    private final CategoryService categoryService;


    @GetMapping("/search")
    public ResponseEntity<List<CategorySearchResponse>> searchCategories(@RequestParam String keyword) {
        // 1. 유효성 검사: 검색어가 없으면 빈 리스트 반환 (200 OK)
        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // 2. 검색 로직 수행
        List<CategorySearchResponse> searchResults = categoryRepository.findTop10ByCategoryNameContaining(keyword)
                .stream()
                .map(c -> new CategorySearchResponse(c.getCategoryId(), c.getCategoryName()))
                .collect(Collectors.toList());

        // 3. 결과 반환 (200 OK)
        return ResponseEntity.status(HttpStatus.OK).body(searchResults);
    }

    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getAllCategories() {
        List<CategoryTreeResponse> responses = categoryService.getCategoryTree();
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

}