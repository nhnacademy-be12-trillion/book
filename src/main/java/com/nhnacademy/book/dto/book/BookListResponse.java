package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;

import java.util.List;
import java.util.stream.Collectors;

public record BookListResponse(
        Long bookId,    // 상세 페이지 이동용
        String bookName,
        String bookAuthor,
        String bookPublisher,
        BookState bookState,
        int bookRegularPrice,
        int bookSalePrice,
        int discountRate,
        double bookReviewRate,
        String bookImage,
        List<String> bookTags
) {

    public static BookListResponse from(Book book, String imageUrl) {
        String authors = book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getAuthorName())
                .collect(Collectors.joining(", "));

        List<String> tagList = book.getBookTags().stream()
                .map(bookTag -> bookTag.getTag().getTagName())
                .collect(Collectors.toList());

        // 할인율 = (정가 - 판매가) / 정가 * 100
        int discountRate = 0;
        if (book.getBookRegularPrice() > 0) { // 0으로 나누기 방지
            discountRate = (int) Math.round(
                    (double) (book.getBookRegularPrice() - book.getBookSalePrice())
                            / book.getBookRegularPrice() * 100
            );
        }
        return new BookListResponse(
                book.getBookId(),
                book.getBookName(),
                authors.isEmpty() ? "작가 미상" : authors, // 작가가 없으면 예외처리
                book.getPublisher() != null ? book.getPublisher().getPublisherName() : "출판사 미상",
                book.getBookState(),
                book.getBookRegularPrice(),
                book.getBookSalePrice(),
                discountRate,
                book.getBookReviewRate(),
                imageUrl, // 여기서 DB가 아닌, 파라미터로 받은 URL을 넣음
                tagList
        );
    }
}