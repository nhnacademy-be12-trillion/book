package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import com.nhnacademy.book.entity.Review;
import com.nhnacademy.book.exception.BookNotFoundException;
import com.nhnacademy.book.exception.DuplicateReviewException;
import com.nhnacademy.book.exception.ReviewAccessDeniedException;
import com.nhnacademy.book.point.PointClient;
import com.nhnacademy.book.repository.BookFileRepository;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.ReviewRepository;
import com.nhnacademy.book.service.FileService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookFileRepository bookFileRepository;
    @Mock
    private MinioService minioService;
    @Mock
    private FileService fileService;
    @Mock
    private PointClient pointClient;

    /**
     * Book 엔티티 생성 도우미 메서드
     */
    private Book createBook(Long bookId) {
        Book book = Book.builder()
                .bookName("테스트 도서")
                .bookStock(10)
                .build();
        ReflectionTestUtils.setField(book, "bookId", bookId);
        return book;
    }

    @Test
    @DisplayName("리뷰 생성 성공 - 이미지 포함")
    void createReview_Success_WithImages() {
        // given
        Long memberId = 1L;
        Long bookId = 10L;
        Long orderId = 100L;

        // [수정] 생성자 파라미터 순서 변경 (orderId, bookId)
        ReviewCreateRequest request = new ReviewCreateRequest(orderId, bookId, 5, "이미지가 있는 리뷰입니다.");

        Book book = createBook(bookId);

        Review review = Review.builder()
                .reviewId(1L)
                .book(book)
                .memberId(memberId)
                .orderId(orderId)
                .reviewRate(5)
                .reviewContents("내용")
                .createdAt(LocalDateTime.now())
                .build();

        MockMultipartFile image = new MockMultipartFile("images", "test.jpg", "image/jpeg", "dummy".getBytes());
        List<MultipartFile> images = List.of(image);

        // Mocking: 명시적으로 bookId 사용
        given(reviewRepository.existsByOrderId(orderId)).willReturn(false);
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(minioService.uploadImage(any(MultipartFile.class))).willReturn("http://minio/test.jpg");
        given(reviewRepository.findAverageRatingByBookId(bookId)).willReturn(5.0);

        // when
        Long savedReviewId = reviewService.createReview(request, images, memberId);

        // then
        assertThat(savedReviewId).isEqualTo(1L);
        verify(minioService, times(1)).uploadImage(any(MultipartFile.class));
        verify(fileService, times(1)).saveReviewImages(eq(1L), anyList());
        verify(pointClient, times(1)).awardReviewPoints(eq(memberId), argThat(pointRequest -> pointRequest.hasPhoto()));
        assertThat(book.getBookReviewRate()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("리뷰 생성 성공 - 이미지 없음")
    void createReview_Success_NoImages() {
        // given
        Long memberId = 1L;
        Long bookId = 10L;
        Long orderId = 100L;
        // [수정] 생성자 파라미터 순서 변경 (orderId, bookId)
        ReviewCreateRequest request = new ReviewCreateRequest(orderId, bookId, 4, "이미지 없는 리뷰");

        Book book = createBook(bookId);
        Review review = Review.builder().reviewId(2L).book(book).build();

        given(reviewRepository.existsByOrderId(orderId)).willReturn(false);
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(reviewRepository.findAverageRatingByBookId(bookId)).willReturn(4.0);

        // when
        reviewService.createReview(request, null, memberId);

        // then
        verify(minioService, never()).uploadImage(any());
        verify(fileService, never()).saveReviewImages(anyLong(), anyList());
        verify(pointClient, times(1)).awardReviewPoints(eq(memberId), argThat(pointRequest -> !pointRequest.hasPhoto()));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 중복된 주문 번호")
    void createReview_Fail_DuplicateOrder() {
        // given
        Long orderId = 100L;
        Long bookId = 1L;
        // [수정] 생성자 파라미터 순서 변경 (orderId, bookId)
        ReviewCreateRequest request = new ReviewCreateRequest(orderId, bookId, 5, "중복 리뷰 시도");
        given(reviewRepository.existsByOrderId(orderId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, null, 1L))
                .isInstanceOf(DuplicateReviewException.class)
                .hasMessageContaining("이미 리뷰를 작성한 주문입니다.");
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 존재하지 않는 도서")
    void createReview_Fail_BookNotFound() {
        // given
        Long orderId = 100L;
        Long nonExistentBookId = 999L;
        // [수정] 생성자 파라미터 순서 변경 (orderId, bookId)
        ReviewCreateRequest request = new ReviewCreateRequest(orderId, nonExistentBookId, 5, "책이 없음");

        given(reviewRepository.existsByOrderId(orderId)).willReturn(false);
        given(bookRepository.findById(nonExistentBookId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, null, 1L))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("도서별 리뷰 목록 조회 - 이미지 매핑 확인")
    void getReviewsByBookId_Success() {
        // given
        Long bookId = 10L;
        Pageable pageable = PageRequest.of(0, 10);
        Book book = createBook(bookId);

        // Book 객체 주입하여 Review 생성
        Review review1 = Review.builder().reviewId(1L).book(book).reviewContents("리뷰1").build();
        Review review2 = Review.builder().reviewId(2L).book(book).reviewContents("리뷰2").build();
        Page<Review> reviewPage = new PageImpl<>(List.of(review1, review2));

        given(reviewRepository.findAllByBook_BookId(bookId, pageable)).willReturn(reviewPage);

        // 이미지 준비
        BookFile file1 = BookFile.builder()
                .fileUrl("http://img/1.jpg")
                .fileType(FileType.REVIEW)
                .joinedId(1L)
                .build();

        given(bookFileRepository.findAllByJoinedIdInAndFileType(anyList(), eq(FileType.REVIEW)))
                .willReturn(List.of(file1));

        // when
        Page<ReviewResponse> result = reviewService.getReviewsByBookId(bookId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent().get(0).reviewContents()).isEqualTo("리뷰1");
        // DTO getter 이름 확인 필요 (imageUrls 또는 reviewImageUrls)
        assertThat(result.getContent().get(0).imageUrls()).contains("http://img/1.jpg");

        assertThat(result.getContent().get(1).reviewContents()).isEqualTo("리뷰2");
        assertThat(result.getContent().get(1).imageUrls()).isEmpty();
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_Success() {
        // given
        Long reviewId = 1L;
        Long memberId = 1L;
        ReviewUpdateRequest request = new ReviewUpdateRequest(3, "수정된 리뷰");

        Book book = createBook(10L);
        Review review = Review.builder()
                .reviewId(reviewId)
                .memberId(memberId)
                .book(book)
                .reviewRate(5)
                .reviewContents("원본")
                .build();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewRepository.findAverageRatingByBookId(book.getBookId())).willReturn(3.0);
        given(bookRepository.findById(book.getBookId())).willReturn(Optional.of(book));

        // when
        reviewService.updateReview(reviewId, request, memberId);

        // then
        assertThat(review.getReviewRate()).isEqualTo(3);
        assertThat(review.getReviewContents()).isEqualTo("수정된 리뷰");
        assertThat(book.getBookReviewRate()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 권한 없음")
    void updateReview_Fail_AccessDenied() {
        // given
        Long reviewId = 1L;
        Long requesterId = 2L;
        Long ownerId = 1L;

        Review review = Review.builder().reviewId(reviewId).memberId(ownerId).build();
        ReviewUpdateRequest request = new ReviewUpdateRequest(3, "수정");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, request, requesterId))
                .isInstanceOf(ReviewAccessDeniedException.class)
                .hasMessageContaining("권한이 없습니다");
    }

    @Test
    @DisplayName("회원별 리뷰 목록 조회")
    void getReviewsByMemberId_Success() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Book book = createBook(10L);

        // Book 주입 필수 (NPE 방지)
        Review review = Review.builder()
                .reviewId(1L)
                .book(book)
                .reviewContents("내 리뷰")
                .build();

        given(reviewRepository.findAllByMemberId(memberId, pageable))
                .willReturn(new PageImpl<>(List.of(review)));
        given(bookFileRepository.findAllByJoinedIdInAndFileType(anyList(), eq(FileType.REVIEW)))
                .willReturn(Collections.emptyList());

        // when
        Page<ReviewResponse> result = reviewService.getReviewsByMemberId(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reviewContents()).isEqualTo("내 리뷰");
    }

    @Test
    @DisplayName("도서 평점 평균 계산 및 업데이트 (반올림 확인)")
    void updateBookAverageRating_Calculation() {
        // given
        Long bookId = 10L;
        Book book = createBook(bookId);

        given(reviewRepository.findAverageRatingByBookId(bookId)).willReturn(4.555);
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // when
        reviewService.updateBookAverageRating(bookId);

        // then
        assertThat(book.getBookReviewRate()).isEqualTo(4.56);
    }

    @Test
    @DisplayName("주문 ID로 리뷰 존재 여부 확인")
    void existsByOrderId_Test() {
        // given
        Long orderId = 100L;
        given(reviewRepository.existsByOrderId(orderId)).willReturn(true);

        // when
        boolean exists = reviewService.existsByOrderId(orderId);

        // then
        assertThat(exists).isTrue();
    }
}