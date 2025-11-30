package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Member;
import com.nhnacademy.book.entity.Wishlist;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.MemberRepository;
import com.nhnacademy.book.repository.WishlistRepository;
import com.nhnacademy.book.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    @Override
    public boolean toggleWishlist(Long memberId, Long bookId) {

        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. ID: " + memberId));

        // 2. 도서 조회
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("도서 정보를 찾을 수 없습니다. ID: " + bookId));

        // 3. 중복 확인 및 토글 로직
        if (wishlistRepository.existsByMemberAndBook(member, book)) {
            // 이미 있으면 삭제 (취소)
            Wishlist wishlist = wishlistRepository.findByMemberAndBook(member, book)
                    .orElseThrow(() -> new IllegalStateException("데이터 불일치: 찜 내역이 존재해야 합니다."));

            wishlistRepository.delete(wishlist);
            return false; // 찜 취소됨 (하트 꺼짐)
        } else {
            // 없으면 저장 (찜 하기)
            Wishlist newWishlist = new Wishlist();
            newWishlist.setMember(member);
            newWishlist.setBook(book);

            wishlistRepository.save(newWishlist);
            return true; // 찜 성공됨 (하트 켜짐)
        }
    }
}