package com.nhnacademy.book.service;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.service.strategy.BookEnrichStrategy;
import com.nhnacademy.book.service.strategy.BookSearchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BookRegistrationService {
    private final BookSearchStrategy bookSearchStrategy;
    private final BookEnrichStrategy bookEnrichStrategy;

    //서비스는 추상적인 요청만.
    public BookCreateRequest getBookInfoByIsbn(String isbn) {
        BookCreateRequest request = bookSearchStrategy.searchBook(isbn);

        if(bookEnrichStrategy.isApplicable(request)) {
            return bookEnrichStrategy.enrich(request);
        }
        return request;
    }

}
