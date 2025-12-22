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

    // [★추가됨] Service에서 books.map(BookListResponse::from)으로 호출할 때 사용되는 메서드
    // 이미지가 아직 준비되지 않았거나 필요 없는 경우를 위해 null을 넣어 호출합니다.
    public static BookListResponse from(Book book) {
        return from(book, null);
    }

    // 기존 메서드 (이미지 URL 포함)
    public static BookListResponse from(Book book, String imageUrl) {
        // 1. 작가 목록 변환
        String authors = book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getAuthorName())
                .collect(Collectors.joining(", "));

        // 2. 태그 목록 변환
        List<String> tagList = book.getBookTags().stream()
                .map(bookTag -> bookTag.getTag().getTagName())
                .collect(Collectors.toList());

        // 3. 할인율 계산
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
                imageUrl, // Service에서 이미지를 조회해서 넣을 경우 사용, 없으면 null
                tagList
        );
    }
}