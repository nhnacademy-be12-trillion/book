package com.nhnacademy.book.dto;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;

import java.time.LocalDate;

public record BookDetailResponse(
//        Long bookId,
//        String isbn,
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
        String bookImage
) {
    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(
                book.getBookName(),
                book.getBookDescription(),
                book.getBookPublisher().getPublisherName(),
                book.getBookPublicationDate(),
                book.getBookIndex(),
                book.isBookPackaging(),
                book.getBookState(),
                book.getBookStock(),
                book.getBookRegularPrice(),
                book.getBookSalePrice(),
                book.getBookReviewRate(),
                book.getBookImage()
        );
    }
}