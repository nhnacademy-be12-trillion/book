package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c.categoryName FROM Category c")
    List<String> findAllCategoryName();
}