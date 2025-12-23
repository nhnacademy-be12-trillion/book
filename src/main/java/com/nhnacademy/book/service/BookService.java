package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {

    // 도서 전체 목록 조회
    Page<BookListResponse> getBooks(Pageable pageable);

    // ID 리스트로 도서 조회
    List<BookListResponse> getBooksByIds(List<Long> bookIds);

    // 인기 도서 조회
    List<BookListResponse> getPopularBooks();

    // [★추가] 전체 신간 도서 조회
    List<BookListResponse> getNewBooks();

    // [★추가] 카테고리별 신간 도서 조회 (Top 5)
    List<BookListResponse> getBooksByCategory(Long categoryId);

    // 도서 상세 조회
    BookDetailResponse getBook(Long bookId);

    // 도서 생성
    Long createBook(BookCreateRequest request, MultipartFile file);

    // 도서 수정
    void updateBook(Long bookId, BookUpdateRequest request);

    // 도서 삭제
    void deleteBook(Long bookId);

    // 조회수 증가
    void increaseViewCount(Long bookId);

    // 할인가 계산
    int calculateSalePrice(int regularPrice, double discountRate);

    // 최상위 카테고리 조회
    List<CategoryTreeResponse> getRootCategories();

    // 카테고리별 도서 페이지 조회
    Page<BookListResponse> getBooksByCategoryPage(Long categoryId, Pageable pageable);
}