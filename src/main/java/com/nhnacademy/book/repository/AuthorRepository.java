package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
