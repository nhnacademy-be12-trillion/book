//package com.nhnacademy.book.service.impl;
//
//import com.nhnacademy.book.dto.book.BookListResponse;
//import com.nhnacademy.book.entity.*;
//import com.nhnacademy.book.exception.BookNotFoundException;
//import com.nhnacademy.book.exception.MemberNotFoundException;
//import com.nhnacademy.book.repository.BookFileRepository;
//import com.nhnacademy.book.repository.BookRepository;
//import com.nhnacademy.book.repository.MemberRepository;
//import com.nhnacademy.book.repository.WishlistRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//class WishlistServiceImplTest {
//
//    @InjectMocks
//    private WishlistServiceImpl wishlistService;
//
//    @Mock
//    private WishlistRepository wishlistRepository;
//
//    @Mock
//    private BookRepository bookRepository;
//
//    @Mock
//    private MemberRepository memberRepository;
//
//    @Mock
//    private BookFileRepository bookFileRepository;
//
//
//    private Book createBook(Long bookId, String title) {
//        // 기본 정보 빌더 생성
//        Book book = Book.builder()
//                .bookName(title)
//                .bookRegularPrice(10000)
//                .bookSalePrice(9000)
//                .bookReviewRate(4.5)
//                .bookState(BookState.ON_SALE)
//                .build();
//
//        // ID 강제 주입
//        ReflectionTestUtils.setField(book, "bookId", bookId);
//
//        // 출판사 설정
//        Publisher publisher = new Publisher("NHN출판");
//        book.assignPublisher(publisher);
//
//        // 작가 설정
//        Author author = new Author("김작가");
//        BookAuthor bookAuthor = new BookAuthor(author, book);
//        book.getBookAuthors().add(bookAuthor);
//
//        // 태그 설정
//        Tag tag = new Tag("베스트셀러");
//        BookTag bookTag = new BookTag(tag, book);
//        book.getBookTags().add(bookTag);
//
//        return book;
//    }
//
//    private Member createMember(Long memberId) {
//        Member member = new Member();
//        ReflectionTestUtils.setField(member, "memberId", memberId); // ID 주입
//        return member;
//    }
//
//
//    @Test
//    @DisplayName("찜 하기 (Toggle On) - 찜 내역이 없을 때 저장하고 true 반환")
//    void toggleWishlist_Add() {
//        // given
//        Long memberId = 1L;
//        Long bookId = 10L;
//
//        Member member = createMember(memberId);
//        Book book = createBook(bookId, "테스트 도서");
//
//        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
//        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
//        given(wishlistRepository.existsByMemberAndBook(member, book)).willReturn(false);
//
//        // when
//        boolean result = wishlistService.toggleWishlist(memberId, bookId);
//
//        // then
//        assertThat(result).isTrue();
//        verify(wishlistRepository).save(any(Wishlist.class));
//    }
//
//    @Test
//    @DisplayName("찜 취소 (Toggle Off) - 찜 내역이 있을 때 삭제하고 false 반환")
//    void toggleWishlist_Remove() {
//        // given
//        Long memberId = 1L;
//        Long bookId = 10L;
//
//        Member member = createMember(memberId);
//        Book book = createBook(bookId, "테스트 도서");
//        Wishlist wishlist = new Wishlist(1L, book, member); // Wishlist 생성자 사용
//
//        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
//        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
//        given(wishlistRepository.existsByMemberAndBook(member, book)).willReturn(true);
//        given(wishlistRepository.findByMemberAndBook(member, book)).willReturn(Optional.of(wishlist));
//
//        // when
//        boolean result = wishlistService.toggleWishlist(memberId, bookId);
//
//        // then
//        assertThat(result).isFalse();
//        verify(wishlistRepository).delete(wishlist);
//    }
//
//    @Test
//    @DisplayName("실패: 존재하지 않는 회원일 경우 예외 발생")
//    void toggleWishlist_MemberNotFound() {
//        // given
//        Long memberId = 99L;
//        Long bookId = 10L;
//
//        given(memberRepository.findById(memberId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> wishlistService.toggleWishlist(memberId, bookId))
//                .isInstanceOf(MemberNotFoundException.class)
//                .hasMessageContaining("회원 정보를 찾을 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("실패: 존재하지 않는 도서일 경우 예외 발생")
//    void toggleWishlist_BookNotFound() {
//        // given
//        Long memberId = 1L;
//        Long bookId = 99L;
//
//        Member member = createMember(memberId);
//        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
//        given(bookRepository.findById(bookId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> wishlistService.toggleWishlist(memberId, bookId))
//                .isInstanceOf(BookNotFoundException.class)
//                .hasMessageContaining("도서 정보를 찾을 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("위시리스트 조회 - 목록이 있을 경우 정상 반환 (이미지 포함 확인)")
//    void getWishlist_Success() {
//        // given
//        Long memberId = 1L;
//        Long bookId1 = 10L;
//        Long bookId2 = 20L;
//
//        Member member = createMember(memberId);
//
//        // 책 생성 (authors, tags는 비어있는 Set 상태)
//        Book book1 = createBook(bookId1, "재밌는 책");
//        Book book2 = createBook(bookId2, "어려운 책");
//
//        // 위시리스트 객체 생성
//        Wishlist wishlist1 = new Wishlist(1L, book1, member);
//        Wishlist wishlist2 = new Wishlist(2L, book2, member);
//
//        given(wishlistRepository.findByMember_MemberId(memberId))
//                .willReturn(List.of(wishlist1, wishlist2));
//
//        // 이미지 파일 Mock 설정 (bookId1만 이미지가 있다고 가정)
//        BookFile bookFile1 = BookFile.builder()
//                .joinedId(bookId1)
//                .fileUrl("image1.jpg")
//                .fileType(FileType.BOOK)
//                .build();
//
//        given(bookFileRepository.findAllByJoinedIdInAndFileType(anyList(), eq(FileType.BOOK)))
//                .willReturn(List.of(bookFile1));
//
//        // when
//        List<BookListResponse> responses = wishlistService.getWishlist(memberId);
//
//        // then
//        assertThat(responses).hasSize(2);
//
//        // 첫 번째 책 검증 (이미지 있음)
//        BookListResponse resp1 = responses.stream()
//                .filter(r -> r.bookId().equals(bookId1))
//                .findFirst().orElseThrow();
//        assertThat(resp1.bookName()).isEqualTo("재밌는 책");
//        assertThat(resp1.bookImage()).isEqualTo("image1.jpg");
//        assertThat(resp1.bookAuthor()).isEqualTo("작가 미상"); // 작가를 추가하지 않았으므로 기본값
//
//        // 두 번째 책 검증 (이미지 없음 -> null)
//        BookListResponse resp2 = responses.stream()
//                .filter(r -> r.bookId().equals(bookId2))
//                .findFirst().orElseThrow();
//        assertThat(resp2.bookName()).isEqualTo("어려운 책");
//        assertThat(resp2.bookImage()).isNull();
//    }
//}