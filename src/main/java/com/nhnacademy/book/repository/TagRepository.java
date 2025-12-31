package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    // 태그 이름으로 태그 단건 조회
    Optional<Tag> findByTagName(String tagName);
}
