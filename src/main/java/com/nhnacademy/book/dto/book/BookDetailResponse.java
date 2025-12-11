package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;

import java.time.LocalDate;
import java.util.stream.Collectors;

public record BookDetailResponse(
        Long bookId,
        String isbn,
        String bookName,
        String bookAuthor,
        String bookDescription,
        String bookPublisher,
        LocalDate bookPublicationDate,
        String bookIndex,
        boolean bookPackaging,
        BookState bookState,
        int bookStock,
        int bookRegularPrice,
        int bookSalePrice,
        int discountRate,
        double bookReviewRate,
        String bookImage,
        int viewCount
) {
    public static BookDetailResponse from(Book book, String imageUrl) {

        // 작가 이름 리스트를 문자열로 변환
        String authors = book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getAuthorName())
                .collect(Collectors.joining(", "));

        // 할인율 계산
        int discountRate = 0;
        if (book.getBookRegularPrice() > 0) {
            discountRate = (int) Math.round(
                    (double) (book.getBookRegularPrice() - book.getBookSalePrice())
                            / book.getBookRegularPrice() * 100
            );
        }

        return new BookDetailResponse(
                book.getBookId(),
                book.getIsbn(),
                book.getBookName(),
                authors.isEmpty() ? "작가 미상" : authors,
                book.getBookDescription(),
                book.getPublisher() != null ? book.getPublisher().getPublisherName() : "출판사 미상",
                book.getBookPublicationDate(),
                book.getBookIndex(),
                book.isBookPackaging(),
                book.getBookState(),
                book.getBookStock(),
                book.getBookRegularPrice(),
                book.getBookSalePrice(),
                discountRate,
                book.getBookReviewRate(),
                imageUrl,
                book.getViewCount()
        );
    }
}