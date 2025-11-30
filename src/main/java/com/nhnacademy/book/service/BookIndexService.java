package com.nhnacademy.book.service;

import org.springframework.data.domain.Pageable;

public interface BookIndexService {

    int augmentBookIndexBatch(Pageable pageable);

    String getTableOfContentsByIsbn(String isbn);
}