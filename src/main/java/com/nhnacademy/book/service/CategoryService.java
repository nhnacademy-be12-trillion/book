package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.category.CategoryTreeResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryTreeResponse> getCategoryTree();
}
