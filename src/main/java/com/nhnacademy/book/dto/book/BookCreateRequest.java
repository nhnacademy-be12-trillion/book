package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.BookState;

import java.time.LocalDate;
import java.util.List;

public record BookCreateRequest(
        String isbn,
        String bookName,
        String bookDescription,
        String bookPublisher,
        String bookAuthor,
        String tags,
        List<Long> categoryIdList,
        LocalDate bookPublicationDate,
        String bookIndex,
        boolean bookPackaging,
        BookState bookState,
        int bookStock,
        int bookRegularPrice,
        int bookSalePrice,
        String bookImage
) {}