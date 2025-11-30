package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Member;
import com.nhnacademy.book.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // Member 객체와 Book 객체를 기준으로 조회
    boolean existsByMemberAndBook(Member member, Book book);
    Optional<Wishlist> findByMemberAndBook(Member member, Book book);
}