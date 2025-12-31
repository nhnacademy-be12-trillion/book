package com.nhnacademy.book.controller.docs;

import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Book", description = "도서 조회 API")
public interface BookControllerDocs {

    @Operation(summary = "전체 도서 목록 조회", description = "전체 도서 목록을 페이징하여 조회합니다.")
    ResponseEntity<Page<BookListResponse>> getBooks(Pageable pageable);

    @Operation(summary = "도서 상세 조회", description = "도서 상세 정보를 조회하고 조회수를 증가시킵니다.")
    ResponseEntity<BookDetailResponse> getBook(
            @Parameter(description = "도서 ID", required = true) @PathVariable("book-id") Long bookId
    );

    @Operation(summary = "주문 대상 도서 목록 조회", description = "장바구니 또는 주문서용 도서 목록을 조회합니다.")
    ResponseEntity<List<BookListResponse>> getBooksForOrder(
            @Parameter(description = "도서 ID 리스트", required = true) @RequestParam List<Long> bookIds,
            @Parameter(description = "수량 리스트", required = true) @RequestParam List<Long> quantities
    );

    @Operation(summary = "베스트셀러 조회", description = "판매량 기준 Top 5 도서를 조회합니다.")
    ResponseEntity<List<BookListResponse>> getBestSellers();

    @Operation(summary = "인기 도서 조회", description = "조회수 기준 인기 도서를 조회합니다.")
    ResponseEntity<List<BookListResponse>> getPopularBooks();

    @Operation(summary = "신간 도서 조회", description = "전체 신간 도서 Top 5를 조회합니다.")
    ResponseEntity<List<BookListResponse>> getNewBooks();

    @Operation(summary = "카테고리별 신간 Top 5 조회", description = "특정 카테고리의 신간 도서 Top 5를 조회합니다.")
    ResponseEntity<List<BookListResponse>> getBooksByCategory(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable("category-id") Long categoryId
    );

    @Operation(summary = "카테고리별 도서 목록 조회 (페이징)", description = "특정 카테고리의 도서 목록을 페이징하여 조회합니다.")
    ResponseEntity<Page<BookListResponse>> getBooksByCategoryPage(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable("category-id") Long categoryId,
            Pageable pageable
    );

    @Operation(summary = "최상위 카테고리 목록 조회", description = "최상위 카테고리 목록을 조회합니다.")
    ResponseEntity<List<CategoryTreeResponse>> getRootCategories();
}
