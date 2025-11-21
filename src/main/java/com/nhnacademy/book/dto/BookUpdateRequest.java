package com.nhnacademy.book.dto;

import com.nhnacademy.book.entity.BookState;

public record BookUpdateRequest(
    String bookName,
    String bookDescription,
    String bookIndex,
    boolean bookPackaging,
    BookState bookState,
    int bookStock,
    double discountRate,
    String bookImage
) {}
