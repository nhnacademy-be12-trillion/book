package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 카테고리 이름에 키워드가 포함된 상위 10개 카테고리 조회
    List<Category> findTop10ByCategoryNameContaining(String keyword);

    // 상위 카테고리(부모가 없는 카테고리) 목록 조회
    List<Category> findAllByParentIsNull();
}