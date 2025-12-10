package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.entity.Category;
import com.nhnacademy.book.repository.CategoryRepository;
import com.nhnacademy.book.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> categoryList = categoryRepository.findAllByParentIsNull();

        return categoryList.stream()
                .map(this::toTreeDto)
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
