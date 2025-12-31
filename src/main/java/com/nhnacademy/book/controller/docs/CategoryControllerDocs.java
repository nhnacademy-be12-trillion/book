package com.nhnacademy.book.controller.docs;

import com.nhnacademy.book.dto.category.BookCategoryResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Category", description = "카테고리 조회 API")
public interface CategoryControllerDocs {

    @Operation(summary = "카테고리 전체 트리 조회", description = "전체 카테고리 구조를 트리 형태로 조회합니다.")
    ResponseEntity<List<CategoryTreeResponse>> getAllCategories();

    @Operation(summary = "도서별 카테고리 조회", description = "특정 도서들의 카테고리 정보를 조회합니다.")
    List<BookCategoryResponse> getBookCategories(
            @Parameter(description = "도서 ID 리스트", required = true) @RequestParam List<Long> bookIds
    );
}
