package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.category.BookWithCategory;
import com.nhnacademy.book.dto.category.CategorySearchResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryTreeResponse> getCategoryTree();

    List<BookWithCategory> getCategoryIds(List<Long> bookIds);
    List<CategorySearchResponse> searchCategories(String keyword);
}
