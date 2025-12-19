package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.category.BookCategoryResponse;
import com.nhnacademy.book.dto.category.CategorySearchResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryTreeResponse> getCategoryTree();

    List<BookCategoryResponse> getCategoryIds(List<Long> bookIds);
    List<CategorySearchResponse> searchCategories(String keyword);
}
