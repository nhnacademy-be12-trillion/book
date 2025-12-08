package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.category.CategoryResponse;
import com.nhnacademy.book.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;


    @GetMapping("/search")
    public List<CategoryResponse> searchCategories(@RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        // 검색어가 포함된 카테고리 10개만 조회 (Repository에 메서드 필요)
        // findTop10ByCategoryNameContaining 메서드가 CategoryRepository에 있어야 함
        return categoryRepository.findTop10ByCategoryNameContaining(keyword)
                .stream()
                .map(c -> new CategoryResponse(c.getCategoryId(), c.getCategoryName()))
                .collect(Collectors.toList());
    }

}