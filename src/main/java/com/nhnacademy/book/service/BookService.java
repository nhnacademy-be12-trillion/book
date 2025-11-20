package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.BookCreateRequest;
import com.nhnacademy.book.dto.BookDetailResponse;
import com.nhnacademy.book.dto.BookListResponse;
import com.nhnacademy.book.dto.BookUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {

    // 1. 도서 목록 조회 (List DTO 사용)
    Page<BookListResponse> getBooks(Pageable pageable);

    // 2. 도서 상세 조회 (Detail DTO 사용)
    BookDetailResponse getBook(Long bookId);

    // 3. 도서 등록 (Create Request DTO 사용)
    Long createBook(BookCreateRequest request);

    // 4. 도서 정보 수정 (Update Request DTO 사용)
    void updateBook(Long bookId, BookUpdateRequest request);

    // 5. 도서 삭제
    void deleteBook(Long bookId);
}