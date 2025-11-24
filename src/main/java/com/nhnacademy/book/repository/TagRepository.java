package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    @Query("SELECT t.tagName FROM Tag t")
    List<String> findAllTagName();
}
