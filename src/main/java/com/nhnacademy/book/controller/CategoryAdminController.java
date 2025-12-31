package com.nhnacademy.book.controller;

import com.nhnacademy.book.controller.docs.CategoryAdminControllerDocs;
import com.nhnacademy.book.dto.category.CategorySearchResponse;
import com.nhnacademy.book.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class CategoryAdminController implements CategoryAdminControllerDocs {

    private final CategoryService categoryService;

    //AI 도서 등록 시 카테고리 검색 API
    // GET /api/admin/categories/search
    @GetMapping("/search")
    public ResponseEntity<List<CategorySearchResponse>> searchCategories(@RequestParam String keyword) {
        // 유효성 검사 - 검색어가 없으면 빈 리스트 반환 (200 OK)
        List<CategorySearchResponse> searchCategories = categoryService.searchCategories(keyword);
        // 결과 반환 (200 OK)
        return ResponseEntity.ok(searchCategories);
    }
}
