package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.category.CategorySearchResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryTreeResponse> getCategoryTree();

    List<CategorySearchResponse> searchCategories(String keyword);
}
