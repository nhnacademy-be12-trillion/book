package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.entity.Publisher;
import com.nhnacademy.book.entity.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager entityManager;

     // 테스트를 위한 더미 Book 생성 및 영속화
    private Book createAndPersistBook() {
        Publisher publisher = null;

        BookState bookState = BookState.ON_SALE;

        Book book = Book.builder()
                .isbn(UUID.randomUUID().toString()) // 유니크 제약조건 위배 방지
                .bookName("Test Book Title")
                .bookDescription("Test Description")
                .bookPublicationDate(LocalDate.now())
                .bookIndex("Index Content")
                .bookPackaging(true)
                .bookState(bookState)
                .bookStock(100)
                .bookRegularPrice(20000)
                .bookSalePrice(18000)
                .bookReviewRate(0.0)
                .publisher(publisher)
                .build();

        return entityManager.persist(book);
    }

    @Test
    @DisplayName("주문 아이디로 리뷰 존재 여부 확인 - 존재할 경우 True")
    void existsByOrderId_ShouldReturnTrue_WhenExists() {
        // given
        Book book = createAndPersistBook();

        Review review = Review.builder()
                .book(book)
                .memberId(1L)
                .orderId(1001L) // Unique Check
                .reviewRate(5)
                .reviewContents("Great book!")
                .build();

        reviewRepository.save(review);

        // when
        boolean exists = reviewRepository.existsByOrderId(1001L);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("주문 아이디로 리뷰 존재 여부 확인 - 존재하지 않을 경우 False")
    void existsByOrderId_ShouldReturnFalse_WhenNotExists() {
        // given
        // 아무 데이터도 저장하지 않음

        // when
        boolean exists = reviewRepository.existsByOrderId(9999L);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("특정 도서의 리뷰 목록 페이징 조회")
    void findAllByBook_BookId_ShouldReturnPagedReviews() {
        // given
        Book book = createAndPersistBook();

        Review review1 = Review.builder().book(book).memberId(1L).orderId(101L).reviewRate(5).reviewContents("Review 1").build();
        Review review2 = Review.builder().book(book).memberId(2L).orderId(102L).reviewRate(4).reviewContents("Review 2").build();
        Review review3 = Review.builder().book(book).memberId(3L).orderId(103L).reviewRate(3).reviewContents("Review 3").build();

        reviewRepository.saveAll(List.of(review1, review2, review3));

        PageRequest pageRequest = PageRequest.of(0, 2); // 0페이지, 2개 조회

        // when
        Page<Review> result = reviewRepository.findAllByBook_BookId(book.getBookId(), pageRequest);

        // then
        assertThat(result.getContent()).hasSize(2); // 2개 가져옴
        assertThat(result.getTotalElements()).isEqualTo(3); // 전체는 3개
        assertThat(result.getContent().get(0).getBook().getBookId()).isEqualTo(book.getBookId());
    }

    @Test
    @DisplayName("도서의 평균 평점 계산")
    void findAverageRatingByBookId_ShouldReturnCorrectAverage() {
        // given
        Book book = createAndPersistBook();

        // 5점, 3점 리뷰 -> 평균 4.0
        Review review1 = Review.builder().book(book).memberId(1L).orderId(201L).reviewRate(5).build();
        Review review2 = Review.builder().book(book).memberId(2L).orderId(202L).reviewRate(3).build();

        reviewRepository.saveAll(List.of(review1, review2));

        // when
        Double avgRate = reviewRepository.findAverageRatingByBookId(book.getBookId());

        // then
        assertThat(avgRate).isEqualTo(4.0);
    }

    @Test
    @DisplayName("리뷰가 없는 도서의 평균 평점은 Null 반환")
    void findAverageRatingByBookId_ShouldReturnNull_WhenNoReviews() {
        // given
        Book book = createAndPersistBook();

        // when
        Double avgRate = reviewRepository.findAverageRatingByBookId(book.getBookId());

        // then
        assertThat(avgRate).isNull();
    }

    @Test
    @DisplayName("특정 회원이 작성한 리뷰 목록 조회")
    void findAllByMemberId_ShouldReturnReviews() {
        // given
        Book book = createAndPersistBook();
        Long targetMemberId = 777L;
        Long otherMemberId = 888L;

        Review myReview = Review.builder().book(book).memberId(targetMemberId).orderId(301L).reviewRate(5).build();
        Review otherReview = Review.builder().book(book).memberId(otherMemberId).orderId(302L).reviewRate(1).build();

        reviewRepository.saveAll(List.of(myReview, otherReview));

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Review> result = reviewRepository.findAllByMemberId(targetMemberId, pageRequest);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMemberId()).isEqualTo(targetMemberId);
    }
}