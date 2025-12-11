package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByAuthorName(String authorName);

}
