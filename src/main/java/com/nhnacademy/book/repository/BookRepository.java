package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT b FROM Book b " +
            "WHERE (b.bookIndex IS NULL OR b.bookIndex = '')")
    List<Book> findAugmentationCandidates(String defaultImageUrl, Pageable pageable);

}
