package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Member;
import com.nhnacademy.book.entity.Wishlist;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.MemberRepository;
import com.nhnacademy.book.repository.WishlistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("찜 하기 (Toggle On) - 찜 내역이 없을 때 저장하고 true 반환")
    void toggleWishlist_Add() {
        // given
        Long memberId = 1L;
        Long bookId = 10L;

        Member member = new Member(); // 필요시 setter로 ID 설정
        Book book = new Book();       // 필요시 setter로 ID 설정

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // 찜 내역이 없음 -> 저장 로직으로 이동
        given(wishlistRepository.existsByMemberAndBook(member, book)).willReturn(false);

        // when
        boolean result = wishlistService.toggleWishlist(memberId, bookId);

        // then
        assertThat(result).isTrue(); // 찜 성공(true)
        verify(wishlistRepository).save(any(Wishlist.class)); // save 호출 확인
        verify(wishlistRepository, never()).delete(any(Wishlist.class)); // delete는 호출되지 않아야 함
    }

    @Test
    @DisplayName("찜 취소 (Toggle Off) - 찜 내역이 있을 때 삭제하고 false 반환")
    void toggleWishlist_Remove() {
        // given
        Long memberId = 1L;
        Long bookId = 10L;

        Member member = new Member();
        Book book = new Book();
        Wishlist wishlist = new Wishlist();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // 찜 내역이 있음 -> 삭제 로직으로 이동
        given(wishlistRepository.existsByMemberAndBook(member, book)).willReturn(true);
        // 삭제를 위해 조회
        given(wishlistRepository.findByMemberAndBook(member, book)).willReturn(Optional.of(wishlist));

        // when
        boolean result = wishlistService.toggleWishlist(memberId, bookId);

        // then
        assertThat(result).isFalse(); // 찜 취소(false)
        verify(wishlistRepository).delete(wishlist); // delete 호출 확인
        verify(wishlistRepository, never()).save(any(Wishlist.class)); // save는 호출되지 않아야 함
    }

    @Test
    @DisplayName("실패: 존재하지 않는 회원일 경우 예외 발생")
    void toggleWishlist_MemberNotFound() {
        // given
        Long memberId = 99L;
        Long bookId = 10L;

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.toggleWishlist(memberId, bookId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("회원 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 존재하지 않는 도서일 경우 예외 발생")
    void toggleWishlist_BookNotFound() {
        // given
        Long memberId = 1L;
        Long bookId = 99L;

        Member member = new Member();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(bookRepository.findById(bookId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.toggleWishlist(memberId, bookId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("도서 정보를 찾을 수 없습니다");
    }
}