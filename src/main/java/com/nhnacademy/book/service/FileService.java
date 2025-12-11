package com.nhnacademy.book.service;

import java.util.List;

public interface FileService {
    void saveBookImage(Long bookId, String fileUrl);

    String getBookImage(Long bookId);

    void saveReviewImages(Long reviewId, List<String> fileUrl);

    List<String> getReviewImages(Long reviewId);
}
