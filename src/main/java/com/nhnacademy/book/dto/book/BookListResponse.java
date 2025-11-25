package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookListResponse {
    private String bookName;
    private String bookPublisher;
    private boolean bookPackaging;
    private BookState bookState;
    private int bookStock;
    private int bookRegularPrice;
    private int bookSalePrice;
    private double bookReviewRate;
    private String bookImage;

    public static BookListResponse from(Book book) {
        return BookListResponse.builder()
                .bookName(book.getBookName())
//                .bookPublisher(book.getBookPublisher())
                .bookPackaging(book.isBookPackaging())
                .bookState(book.getBookState())
                .bookStock(book.getBookStock())
                .bookRegularPrice(book.getBookRegularPrice())
                .bookSalePrice(book.getBookSalePrice())
                .bookReviewRate(book.getBookReviewRate())
                .bookImage(book.getBookImage())
                .build();

    }
}
