package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT b FROM Book b " +
            "WHERE (b.bookIndex IS NULL OR b.bookIndex = '')")
    List<Book> findAugmentationCandidates(String defaultImageUrl, Pageable pageable);

    @Modifying
    @Query("UPDATE Book b SET b.viewCount = b.viewCount + 1 WHERE b.bookId = :bookId")
    void updateViewCount(@Param("bookId") Long bookId);

    // 마이그레이션용: 이미지 URL이 'http'로 시작하는(아직 안 옮긴) 책 조회
    // List<Book> findByBookImageStartingWith(String prefix);

    // 이미지가 아직 남아있는(NULL이 아닌) 책들만 조회 -> 마이그레이션 대상
    List<Book> findByBookImageIsNotNull();
}
