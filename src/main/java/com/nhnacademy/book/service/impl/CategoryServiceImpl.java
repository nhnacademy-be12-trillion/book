package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.category.BookWithCategory;
import com.nhnacademy.book.dto.category.CategorySearchResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.entity.Category;
import com.nhnacademy.book.repository.BookCategoryRepository;
import com.nhnacademy.book.repository.CategoryRepository;
import com.nhnacademy.book.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> categoryList = categoryRepository.findAllByParentIsNull();

        return categoryList.stream()
                .map(this::toTreeDto)
                .toList();
    }

    //해당 값은 dto
    @Override
    @Transactional(readOnly = true)
    public List<BookWithCategory> getCategoryIds(List<Long> bookIds) {
        return bookCategoryRepository.findBookCategoryIds(bookIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySearchResponse> searchCategories(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }

        return categoryRepository.findTop10ByCategoryNameContaining(keyword)
                .stream()
                // Entity -> SearchDTO 변환
                .map(c -> new CategorySearchResponse(c.getCategoryId(), c.getCategoryName()))
                .toList();
    }

    private CategoryTreeResponse toTreeDto(Category category) {
        return new CategoryTreeResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getChildren().stream()
                        .map(this::toTreeDto)
                        .toList()
        );
    }
}
