package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 도서의 리뷰 목록 페이징 조회
    Page<Review> findAllByBook_BookId(Long bookId, Pageable pageable);

    // 해당 도서의 평균 평점을 계산하는 JPQL 쿼리
    @Query("SELECT AVG(r.reviewRate) FROM Review r WHERE r.book.bookId = :bookId")
    Double findAverageRatingByBookId(@Param("bookId") Long bookId);
}