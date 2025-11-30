package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Member;
import com.nhnacademy.book.entity.Review;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.MemberRepository;
import com.nhnacademy.book.repository.ReviewRepository;
import com.nhnacademy.book.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    // 리뷰 작성
    @Override
    @Transactional
    public Long createReview(ReviewCreateRequest request, Long memberId) {

        // 도서 및 회원 존재 확인 (Gateway ID는 신뢰하지만, DB에 객체가 있어야 JPA 연결 가능)
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서입니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // TODO: [비즈니스 검증] 사용자가 이 책을 구매했는지 확인하는 로직 추가

        // Review 엔티티 생성
        Review review = Review.builder()
                .book(book)
                .member(member)
                .reviewRate(request.reviewRate())
                .reviewContents(request.reviewContents())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);

        // 평점 업데이트
        updateBookAverageRating(book.getBookId());

        return savedReview.getReviewId();
    }

    // 특정 도서의 리뷰 목록 조회
    @Override
    public Page<ReviewResponse> getReviewsByBookId(Long bookId, Pageable pageable) {

        return reviewRepository.findAllByBook_BookId(bookId, pageable)
                .map(ReviewResponse::from);
    }

    // 리뷰 수정
    @Override
    @Transactional
    public void updateReview(Long reviewId, ReviewUpdateRequest request, Long memberId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

        // 소유권 검증 로직
        // Gateway ID와 리뷰 작성자 ID가 다르면 권한 없음
        if (!review.getMember().getMemberId().equals(memberId)) {
            // Global Exception Handler가 403을 반환하도록 식별자를 포함한 RuntimeException 사용
            throw new RuntimeException("AUTHORIZATION_FAILURE: 리뷰 수정 권한이 없습니다. (작성자 ID 불일치)");
        }

        // Review 엔티티 수정 (JPA 변경 감지)
        review.update(
                request.reviewRate(),
                request.reviewContents()
        );

        // 평점 재계산
        updateBookAverageRating(review.getBook().getBookId());
    }

    // Book 엔티티의 평균 평점을 업데이트
    @Override
    @Transactional
    public void updateBookAverageRating(Long bookId) {
        // 해당 도서의 모든 리뷰 평점의 평균을 DB에서 직접 계산
        Double averageRating = reviewRepository.findAverageRatingByBookId(bookId);

        // Book 엔티티 조회
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("도서가 존재하지 않습니다."));

        // Book 엔티티의 평점 필드 업데이트
        book.setBookReviewRate(averageRating != null ? Math.round(averageRating * 100.0) / 100.0 : 0.0);
    }
}