package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;

import java.time.LocalDate;

public record BookDetailResponse(
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
        double bookReviewRate,
        String bookImage,
        int viewCount
) {
    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(
                book.getBookName(),
                book.getBookDescription(),
                book.getPublisher().getPublisherName(),
                book.getBookPublicationDate(),
                book.getBookIndex(),
                book.isBookPackaging(),
                book.getBookState(),
                book.getBookStock(),
                book.getBookRegularPrice(),
                book.getBookSalePrice(),
                book.getBookReviewRate(),
                book.getBookImage(),
                book.getViewCount()
        );
    }
}