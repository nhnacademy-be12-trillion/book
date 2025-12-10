package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.repository.BookFileRepository;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.MemberRepository;
import com.nhnacademy.book.repository.ReviewRepository;
import com.nhnacademy.book.service.FileService;
import com.nhnacademy.book.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BookFileRepository bookFileRepository;
    private final MinioService minioService;
    private final FileService fileService;

    // 리뷰 작성
    @Override
    @Transactional
    public Long createReview(ReviewCreateRequest request, List<MultipartFile> images, Long memberId) {

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

        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    // (1) MinIO에 업로드하고 URL 받기
                    String imageUrl = minioService.uploadImage(image);
                    imageUrls.add(imageUrl);
                }
            }
            // (2) BookFile 테이블에 저장 (FileType.REVIEW, joinedId = 리뷰ID)
            fileService.saveReviewImages(savedReview.getReviewId(),imageUrls);
        }

        // 평점 업데이트
        updateBookAverageRating(book.getBookId());

        return savedReview.getReviewId();
    }

    // 특정 도서의 리뷰 목록 조회
    @Override
    public Page<ReviewResponse> getReviewsByBookId(Long bookId, Pageable pageable) {
        // 리뷰 목록 조회
        Page<Review> reviews = reviewRepository.findAllByBook_BookId(bookId, pageable);

        // 리뷰가 하나도 없으면 빈 페이지 반환
        if (reviews.isEmpty()) {
            return Page.empty(pageable);
        }

        // 조회된 리뷰들의 ID만 추출
        List<Long> reviewIds = reviews.getContent().stream()
                .map(Review::getReviewId)
                .toList();

        List<BookFile> fileList = bookFileRepository.findAllByFileTypeAndJoinedIdIn(FileType.REVIEW,reviewIds);

        // 조회한 이미지들을 '리뷰 ID'를 키(Key)로 하는 맵(Map)으로 변환 (메모리 작업)
        // 구조: Map<리뷰ID, List<이미지URL>>
        Map<Long, List<String>> reviewImageMap = fileList.stream()
                .collect(Collectors.groupingBy(
                        BookFile::getJoinedId, // Key: 리뷰 ID로 그룹화
                        Collectors.mapping(BookFile::getFileUrl, Collectors.toList()) // Value: URL 리스트로 변환
                ));

        // 이제 조립만 하면 끝 (DB 조회 없음, 메모리에서 Map.get으로 꺼냄)
        return reviews.map(review -> {
            // 맵에서 내 ID에 맞는 이미지 리스트 꺼내기 (없으면 빈 리스트)
            List<String> imageUrls = reviewImageMap.getOrDefault(review.getReviewId(), Collections.emptyList());

            return ReviewResponse.from(review, imageUrls);
        });
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

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByMemberId(Long memberId, Pageable pageable){
        Page<Review> reviews = reviewRepository.findAllByMember_MemberId(memberId, pageable);

        // 리뷰가 없으면 빈 페이지 반환
        if (reviews.isEmpty()) {
            return Page.empty(pageable);
        }

        // 이미지 조회 (N+1 방지 로직 재사용)
        // 조회된 리뷰들의 ID 추출
        List<Long> reviewIds = reviews.getContent().stream()
                .map(Review::getReviewId)
                .toList();

        // 파일 리포지토리에서 리뷰 ID들에 해당하는 이미지 일괄 조회
        List<BookFile> fileList = bookFileRepository.findAllByFileTypeAndJoinedIdIn(FileType.REVIEW, reviewIds);

        // 메모리 그룹핑 (Map<리뷰ID, 이미지URL리스트>)
        Map<Long, List<String>> reviewImageMap = fileList.stream()
                .collect(Collectors.groupingBy(
                        BookFile::getJoinedId,
                        Collectors.mapping(BookFile::getFileUrl, Collectors.toList())
                ));

        // DTO 변환 및 반환
        return reviews.map(review -> {
            List<String> imageUrls = reviewImageMap.getOrDefault(review.getReviewId(), Collections.emptyList());
            // 필요한 경우 ReviewResponse에 '책 제목'이나 '책 이미지' 정보가 필요할 수 있음
            // 현재 DTO 구조상으로는 리뷰 내용과 이미지 위주로 반환됨
            return ReviewResponse.from(review, imageUrls);
        });

    }


}