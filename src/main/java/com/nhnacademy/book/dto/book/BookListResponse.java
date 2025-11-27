package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;

public record BookListResponse(
        String bookName,
        String bookPublisher,
        boolean bookPackaging,
        BookState bookState,
        int bookStock,
        int bookRegularPrice,
        int bookSalePrice,
        double bookReviewRate,
        String bookImage
) {

    public static BookListResponse from(Book book) {
        return new BookListResponse(
                book.getBookName(),
                book.getPublisher().getPublisherName(),
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