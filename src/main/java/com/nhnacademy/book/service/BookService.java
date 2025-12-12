package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {

    // 도서 목록 조회 (List DTO 사용)
    Page<BookListResponse> getBooks(Pageable pageable);

    // [지금 많이 보는 도서] 5개 조회
    List<BookListResponse> getPopularBooks();

    // 카테고리별 신간 5개 조회
    List<BookListResponse> getBooksByCategory(Long categoryId);

    // 도서 상세 조회 (Detail DTO 사용)
    BookDetailResponse getBook(Long bookId);

    // 도서 등록 (Create Request DTO 사용)
    Long createBook(BookCreateRequest request, MultipartFile file);

    // 도서 정보 수정 (Update Request DTO 사용)
    void updateBook(Long bookId, BookUpdateRequest request);

    // 도서 삭제
    void deleteBook(Long bookId);

    // 도서 조회수
    void increaseViewCount(Long bookId);

    // 도서 수량
    public void deductStock(Long bookId, int quantity);

    int calculateSalePrice(int regularPrice, double discountRate);

}