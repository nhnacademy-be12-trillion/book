package com.nhnacademy.book.service.strategy;

import com.nhnacademy.book.dto.book.BookCreateRequest;

public interface BookEnrichStrategy {

    //ai 가 부족한 정보를 채워줄 필요가 있는지 여부
    boolean isApplicable(BookCreateRequest request);
    BookCreateRequest enrich(BookCreateRequest request); // 정보 보강 실해
}
