package com.nhnacademy.book.dto.review;

public record ReviewCreateRequest (
    Long orderId,
    Long bookId,
    int reviewRate,
    String reviewContents
){}