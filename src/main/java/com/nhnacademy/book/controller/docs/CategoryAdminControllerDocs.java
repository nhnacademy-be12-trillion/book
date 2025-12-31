package com.nhnacademy.book.controller.docs;

import com.nhnacademy.book.dto.category.CategorySearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Admin Category", description = "관리자 카테고리 관리 API")
public interface CategoryAdminControllerDocs {

    @Operation(summary = "카테고리 검색", description = "AI 도서 등록 시 카테고리를 검색합니다.")
    ResponseEntity<List<CategorySearchResponse>> searchCategories(
            @Parameter(description = "검색어", required = true) String keyword
    );
}
