package com.nhnacademy.book.service.strategy;

import com.nhnacademy.book.dto.book.BookCreateRequest;

public interface BookSearchStrategy {

    //도서 검색 전략..
    BookCreateRequest searchBook(String isbn);
}
