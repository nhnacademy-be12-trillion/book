package com.nhnacademy.book.dto.category;

import java.util.List;

public record CategoryTreeResponse(
        Long categoryId,
        String categoryName,
        List<CategoryTreeResponse> children
) {
}