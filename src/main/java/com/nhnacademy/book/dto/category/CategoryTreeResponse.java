package com.nhnacademy.book.dto.category;

import java.util.List;
import java.util.stream.LongStream;

public record CategoryTreeResponse(
        Long categoryId,
        String categoryName,
        List<CategoryTreeResponse> children
) {
}