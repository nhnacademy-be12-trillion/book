package com.nhnacademy.book.dto;

import com.nhnacademy.book.entity.BookState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookUpdateRequest {
    // 고유값 등 수정하지 않는 값들 제외
    private String bookName;
    private String bookDescription;
    private String bookIndex;
    private boolean bookPackaging;
    private BookState bookState;
    private int bookStock;

    private double discountRate;

    private String bookImage;
}
