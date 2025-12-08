package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    @Query("SELECT t.tagName FROM Tag t")
    List<String> findAllTagName();

    Optional<Tag> findByTagName(String tagName);
}
