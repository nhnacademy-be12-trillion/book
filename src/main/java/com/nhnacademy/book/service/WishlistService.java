package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.book.BookListResponse;

import java.util.List;

public interface WishlistService {

    // 찜 토글 (추가/삭제)
    boolean toggleWishlist(Long memberId, Long bookId);

    // 찜 목록 조회
    List<BookListResponse> getWishlist(Long memberId);
}