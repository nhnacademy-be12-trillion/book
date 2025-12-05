package com.nhnacademy.book.service;

public interface FileService {
    void saveBookImage(Long bookId, String fileUrl);

    String getBookImage(Long bookId);
}
