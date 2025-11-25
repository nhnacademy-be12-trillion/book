package com.nhnacademy.book.dto.review;

public record ReviewUpdateRequest(
    int reviewRate,
    String reviewContents
) {}