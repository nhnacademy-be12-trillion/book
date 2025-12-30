package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.exception.BookNotFoundException;
import com.nhnacademy.book.exception.WishlistNotFoundException;
import com.nhnacademy.book.repository.BookFileRepository;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.WishlistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookFileRepository bookFileRepository;

    // 더미 데이터 생성 헬퍼
    private Book createBook(Long id, BookState state) {
        // Reflection을 사용하여 ID 세팅 (Setter가 없거나 Protected인 경우)
        Book book = Book.builder()
                .bookName("Test Book")
                .bookState(state)
                .bookPublicationDate(LocalDate.now())
                .bookRegularPrice(10000)
                .bookSalePrice(9000)
                .build();

        // ID 강제 주입 (테스트용)
        try {
            var field = Book.class.getDeclaredField("bookId");
            field.setAccessible(true);
            field.set(book, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return book;
    }

    @Test
    @DisplayName("찜 토글 - 도서가 존재하지 않을 때 예외 발생")
    void toggleWishlist_BookNotFound() {
        // given
        Long memberId = 1L;
        Long bookId = 999L;
        given(bookRepository.findById(bookId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.toggleWishlist(memberId, bookId))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("도서 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("찜 토글 - 찜하기 (신규 등록)")
    void toggleWishlist_Add() {
        // given
        Long memberId = 1L;
        Long bookId = 10L;
        Book book = createBook(bookId, BookState.ON_SALE);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(wishlistRepository.existsByMemberIdAndBook(memberId, book)).willReturn(false);

        // when
        boolean result = wishlistService.toggleWishlist(memberId, bookId);

        // then
        assertThat(result).isTrue(); // 찜 성공 시 true
        verify(wishlistRepository, times(1)).save(any(Wishlist.class)); // save 호출 확인
    }

    @Test
    @DisplayName("찜 토글 - 찜 취소 (삭제)")
    void toggleWishlist_Remove() {
        // given
        Long memberId = 1L;
        Long bookId = 10L;
        Book book = createBook(bookId, BookState.ON_SALE);
        Wishlist wishlist = Wishlist.create(memberId, book);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(wishlistRepository.existsByMemberIdAndBook(memberId, book)).willReturn(true);
        given(wishlistRepository.findByMemberIdAndBook(memberId, book)).willReturn(Optional.of(wishlist));

        // when
        boolean result = wishlistService.toggleWishlist(memberId, bookId);

        // then
        assertThat(result).isFalse(); // 찜 취소 시 false
        verify(wishlistRepository, times(1)).delete(wishlist); // delete 호출 확인
    }

    @Test
    @DisplayName("찜 토글 - 데이터 불일치 예외 (exists는 true인데 find는 empty)")
    void toggleWishlist_InconsistentData() {
        // given
        Long memberId = 1L;
        Long bookId = 10L;
        Book book = createBook(bookId, BookState.ON_SALE);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(wishlistRepository.existsByMemberIdAndBook(memberId, book)).willReturn(true);
        given(wishlistRepository.findByMemberIdAndBook(memberId, book)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.toggleWishlist(memberId, bookId))
                .isInstanceOf(WishlistNotFoundException.class)
                .hasMessageContaining("데이터 불일치");
    }

    @Test
    @DisplayName("위시리스트 조회 - 정상 조회 (이미지 포함)")
    void getWishlist_Success() {
        // given
        Long memberId = 1L;
        Book book = createBook(100L, BookState.ON_SALE);
        Wishlist wishlist = Wishlist.create(memberId, book);
        String expectedImageUrl = "http://example.com/image.jpg";

        BookFile bookFile = BookFile.builder()
                .fileUrl(expectedImageUrl)
                .fileType(FileType.BOOK)
                .joinedId(book.getBookId())
                .build();

        given(wishlistRepository.findByMemberId(memberId)).willReturn(List.of(wishlist));
        given(bookFileRepository.findAllByJoinedIdInAndFileType(anyList(), eq(FileType.BOOK)))
                .willReturn(List.of(bookFile));

        // when
        List<BookListResponse> responses = wishlistService.getWishlist(memberId);

        // then
        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("위시리스트 조회 - 위시리스트가 없을 때")
    void getWishlist_Empty() {
        // given
        Long memberId = 1L;
        given(wishlistRepository.findByMemberId(memberId)).willReturn(Collections.emptyList());

        // when
        List<BookListResponse> responses = wishlistService.getWishlist(memberId);

        // then
        assertThat(responses).isEmpty();
        verify(bookFileRepository, never()).findAllByJoinedIdInAndFileType(any(), any());
    }

    @Test
    @DisplayName("위시리스트 조회 - 판매 종료(SALE_END) 도서 자동 삭제 및 필터링")
    void getWishlist_FilterSaleEnd() {
        // given
        Long memberId = 1L;
        Book activeBook = createBook(100L, BookState.ON_SALE);
        Book expiredBook = createBook(200L, BookState.SALE_END);

        Wishlist activeWishlist = Wishlist.create(memberId, activeBook);
        Wishlist expiredWishlist = Wishlist.create(memberId, expiredBook);

        // Mock 리턴: 두 개의 찜 내역 반환
        given(wishlistRepository.findByMemberId(memberId))
                .willReturn(List.of(activeWishlist, expiredWishlist));

        // 활성 도서에 대한 이미지만 요청될 것임
        given(bookFileRepository.findAllByJoinedIdInAndFileType(anyList(), eq(FileType.BOOK)))
                .willReturn(Collections.emptyList());

        // when
        List<BookListResponse> responses = wishlistService.getWishlist(memberId);

        // then
        assertThat(responses).hasSize(1); // 판매 종료된 건 제외되어야 함

        // 판매 종료된 위시리스트는 delete 메서드가 호출되었는지 검증
        verify(wishlistRepository).delete(expiredWishlist);
        verify(wishlistRepository, never()).delete(activeWishlist);
    }
}