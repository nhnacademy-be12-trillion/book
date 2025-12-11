package com.nhnacademy.book.controller;

import com.nhnacademy.book.service.FileService;
import com.nhnacademy.book.service.impl.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
//@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService;
    private final MinioService minioService;

    @PostMapping("/books/{bookId}/upload")
    public ResponseEntity<String> uploadBookImage(
            @PathVariable Long bookId,
            @RequestPart("file") MultipartFile file
    ) {
        // MinIO에 파일 업로드하고 URL 받기
        String uploadedUrl = minioService.uploadImage(file);

        // DB에 도서 ID와 URL 연결 정보 저장
        fileService.saveBookImage(bookId, uploadedUrl);

        // 결과 반환
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("성공! MinIO URL: " + uploadedUrl);
    }

}
