package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.BookState;

import java.time.LocalDate;

public record BookCreateRequest(
        String isbn,
        String bookName,
        String bookDescription,
        String bookPublisher,
        LocalDate bookPublicationDate,
        String bookIndex,
        boolean bookPackaging,
        BookState bookState,
        int bookStock,
        int bookRegularPrice,
        int bookSalePrice,
        String bookImage
) {}