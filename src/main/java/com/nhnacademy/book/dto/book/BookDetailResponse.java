package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class BookDetailResponse {
    private Long bookId;
    private String isbn;
    private String bookName;
    private String bookDescription;
    private String bookPublisher;
    private LocalDate bookPublicationDate;
    private String bookIndex;
    private boolean bookPackaging;
    private BookState bookState;
    private int bookStock;
    private int bookRegularPrice;
    private int bookSalePrice;
    private double bookReviewRate;
    private String bookImage;

    public static BookDetailResponse from(Book book) {
        return BookDetailResponse.builder()
                .bookId(book.getBookId())
                .isbn(book.getIsbn())
                .bookName(book.getBookName())
                .bookDescription(book.getBookDescription())
//                .bookPublisher(book.getBookPublisher())
                .bookPublicationDate(book.getBookPublicationDate())
                .bookIndex(book.getBookIndex())
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
