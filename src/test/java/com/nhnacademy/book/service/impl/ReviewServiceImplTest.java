package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Member;
import com.nhnacademy.book.entity.Review;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.MemberRepository;
import com.nhnacademy.book.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("리뷰 작성 성공 - 저장 및 평점 업데이트 확인")
    void createReview_Success() {
        // given
        Long memberId = 1L;
        Long bookId = 10L;
        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 5, "재미있어요!");

        Book book = new Book();
        book.setBookId(bookId);

        Member member = new Member();
        member.setMemberId(memberId);

        // 저장될 리뷰 객체 (Mock 반환용)
        Review savedReview = Review.builder()
                .book(book)
                .member(member)
                .reviewRate(5)
                .reviewContents("재미있어요!")
                .build();
        // ID 강제 주입 (리플렉션이나 setter 필요하지만, 여기선 Mockito가 객체 자체를 반환하므로 getter에서 쓸 수 있게 설정한다고 가정하거나, Test용 객체 사용)
        // 실제로는 JPA가 ID를 할당하지만, Test에서는 리턴값을 검증하므로 그대로 사용

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(reviewRepository.save(any(Review.class))).willReturn(savedReview);

        // 평점 계산 Mock (평균 5.0이라고 가정)
        given(reviewRepository.findAverageRatingByBookId(bookId)).willReturn(5.0);

        // when
        reviewService.createReview(request, memberId);

        // then
        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).findAverageRatingByBookId(bookId);

        // Book 엔티티의 평점이 업데이트 되었는지 확인
        assertThat(book.getBookReviewRate()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 존재하지 않는 도서")
    void createReview_BookNotFound() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(99L, 5, "내용");
        given(bookRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 도서");
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 존재하지 않는 회원")
    void createReview_MemberNotFound() {
        // given
        Long bookId = 1L;
        Long memberId = 99L;
        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 5, "내용");

        given(bookRepository.findById(bookId)).willReturn(Optional.of(new Book()));
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 회원");
    }

    @Test
    @DisplayName("리뷰 수정 성공 - 작성자 본인 확인 및 평점 재계산")
    void updateReview_Success() {
        // given
        Long reviewId = 1L;
        Long memberId = 1L; // 작성자 ID
        Long bookId = 10L;

        Member writer = new Member();
        writer.setMemberId(memberId);

        Book book = new Book();
        book.setBookId(bookId);

        // 기존 리뷰
        Review review = Review.builder()
                .member(writer)
                .book(book)
                .reviewRate(5)
                .reviewContents("원래 내용")
                .build();

        // 수정 요청
        ReviewUpdateRequest request = new ReviewUpdateRequest(3, "수정된 내용");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        // 평점 재계산 Mock (수정 후 평균 3.0 가정)
        given(reviewRepository.findAverageRatingByBookId(bookId)).willReturn(3.0);
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));


        // when
        reviewService.updateReview(reviewId, request, memberId);

        // then
        // 1. 내용 변경 확인 (Dirty Checking을 위한 엔티티 상태 변경)
        assertThat(review.getReviewContents()).isEqualTo("수정된 내용");
        assertThat(review.getReviewRate()).isEqualTo(3);

        // 2. 평점 재계산 로직 호출 확인
        verify(reviewRepository).findAverageRatingByBookId(bookId);
        assertThat(book.getBookReviewRate()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 작성자가 아님 (권한 없음)")
    void updateReview_Unauthorized() {
        // given
        Long reviewId = 1L;
        Long ownerId = 1L;
        Long otherUserId = 2L; // 다른 사용자

        Member writer = new Member();
        writer.setMemberId(ownerId);

        Review review = Review.builder()
                .member(writer) // 작성자는 1번
                .build();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        ReviewUpdateRequest request = new ReviewUpdateRequest(1, "해킹 시도");

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, request, otherUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AUTHORIZATION_FAILURE");
    }

    @Test
    @DisplayName("도서별 리뷰 목록 조회")
    void getReviewsByBookId_Success() {
        // given
        Long bookId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        // Mocking을 위한 더미 데이터
        Member member = new Member();
        member.setMemberId(1L);
        member.setName("testUser"); // Response 변환 시 필요할 수 있음

        Review review = Review.builder()
                .reviewRate(5)
                .reviewContents("좋아요")
                .member(member)
                .build();

        Page<Review> reviewPage = new PageImpl<>(List.of(review));

        given(reviewRepository.findAllByBook_BookId(bookId, pageable)).willReturn(reviewPage);

        // when
        Page<ReviewResponse> result = reviewService.getReviewsByBookId(bookId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reviewContents()).isEqualTo("좋아요");
    }

    @Test
    @DisplayName("평점 평균 업데이트 로직 검증 (리뷰 삭제 등에서 활용될 로직)")
    void updateBookAverageRating_CalculateCorrectly() {
        // given
        Long bookId = 10L;
        Book book = new Book();
        book.setBookId(bookId);
        book.setBookReviewRate(0.0); // 초기값

        given(reviewRepository.findAverageRatingByBookId(bookId)).willReturn(4.555); // 소수점 셋째자리
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // when
        reviewService.updateBookAverageRating(bookId);

        // then
        // Math.round(4.555 * 100.0) / 100.0 => 455.5 반올림 => 456 / 100.0 => 4.56
        assertThat(book.getBookReviewRate()).isEqualTo(4.56);
    }
}