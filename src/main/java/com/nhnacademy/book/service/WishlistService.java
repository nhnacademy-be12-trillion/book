package com.nhnacademy.book.service;

public interface WishlistService {

    boolean toggleWishlist(Long memberId, Long bookId);
}