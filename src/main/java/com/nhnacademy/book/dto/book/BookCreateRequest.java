package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.BookState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class BookCreateRequest {
    // bookId, bookReviewRate 등 값을 직접 넣는 게 아닌 컬럼 제외
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
    private String bookImage;
}
