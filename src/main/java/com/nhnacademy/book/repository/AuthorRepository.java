package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    // 저자 이름으로 저자 단건 조회
    Optional<Author> findByAuthorName(String authorName);

}
