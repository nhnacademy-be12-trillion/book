package com.nhnacademy.book.controller.docs;

import com.nhnacademy.book.dto.review.ReviewCreateRequest;
import com.nhnacademy.book.dto.review.ReviewResponse;
import com.nhnacademy.book.dto.review.ReviewUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Review", description = "리뷰 관리 API")
public interface ReviewControllerDocs {

    @Operation(summary = "리뷰 등록", description = "도서에 대한 리뷰를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "리뷰 등록 성공", content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "이미 리뷰가 존재함")
    })
    ResponseEntity<Long> createReview(
            @Parameter(description = "리뷰 생성 정보", required = true) ReviewCreateRequest request,
            @Parameter(description = "리뷰 이미지 파일 목록") List<MultipartFile> images,
            @Parameter(description = "회원 ID", required = true) Long memberId
    );

    @Operation(summary = "리뷰 수정", description = "작성한 리뷰를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "리뷰 수정 성공"),
            @ApiResponse(responseCode = "403", description = "리뷰 수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    ResponseEntity<Void> updateReview(
            @Parameter(description = "리뷰 ID", required = true) Long reviewId,
            @Parameter(description = "리뷰 수정 정보", required = true) ReviewUpdateRequest request,
            @Parameter(description = "회원 ID", required = true) Long memberId
    );

    @Operation(summary = "도서별 리뷰 목록 조회", description = "특정 도서의 리뷰 목록을 조회합니다.")
    ResponseEntity<Page<ReviewResponse>> getReviewsByBookId(
            @Parameter(description = "도서 ID", required = true) Long bookId,
            Pageable pageable
    );

    @Operation(summary = "내 리뷰 목록 조회", description = "내가 작성한 리뷰 목록을 조회합니다.")
    ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @Parameter(description = "회원 ID", required = true) Long memberId,
            Pageable pageable
    );

    @Operation(summary = "리뷰 작성 여부 확인", description = "해당 주문에 대해 이미 리뷰를 작성했는지 확인합니다.")
    ResponseEntity<Boolean> checkReviewExistence(
            @Parameter(description = "주문 ID", required = true) Long orderId
    );
}
