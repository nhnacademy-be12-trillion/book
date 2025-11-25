package com.nhnacademy.book.dto.review;

public record ReviewCreateRequest (
    Long bookId,
    int reviewRate,
    String reviewContents
){}