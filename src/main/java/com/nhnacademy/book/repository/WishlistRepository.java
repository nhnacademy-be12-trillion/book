package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 특정 회원의 특정 책 찜 여부 확인
    boolean existsByMemberIdAndBook(Long memberId, Book book);

    // 특정 회원의 특정 책 찜 엔티티 조회 (삭제 시 사용)
    Optional<Wishlist> findByMemberIdAndBook(Long memberId, Book book);

    // 특정 회원의 모든 위시리스트 조회
    List<Wishlist> findByMemberId(Long memberId);
}