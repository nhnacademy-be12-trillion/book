package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.exception.ExternalApiCallException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    // 프론트 파일 업로드용
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            InputStream inputStream = file.getInputStream();
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();

            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            String baseUrl = minioUrl.endsWith("/") ? minioUrl : minioUrl + "/";
            return baseUrl + bucketName + "/" + fileName;

        } catch (Exception e) {
            log.error("MinIO 이미지 업로드 실패", e);
            throw new ExternalApiCallException("이미지 업로드에 실패했습니다.");
        }
    }

    // URL 이미지 다운로드 및 업로드
    public String uploadFromUrl(String imageUrl) {
        try {
            // --- 1단계: 알라딘에서 이미지 다운로드 (메모리에 저장) ---
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 차단 우회 헤더
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.aladin.co.kr/");
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000); // 5초 (너무 길면 줄이세요)
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
              throw new ExternalApiCallException("외부 이미자 다운 실패");
            }

            // 이미지를 byte 배열로 한 번에 읽어옴
            byte[] imageBytes;
            String contentType;
            try (InputStream is = conn.getInputStream()) {
                imageBytes = is.readAllBytes();
                contentType = conn.getContentType();
            }

            // --- 2단계: MinIO로 업로드 ---
            String extension = (contentType != null && contentType.contains("png")) ? ".png" : ".jpg";
            String fileName = UUID.randomUUID().toString() + extension;

            // 바이트 배열을 스트림으로 변환
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(bais, imageBytes.length, -1) // 이제 정확한 파일 크기(imageBytes.length)를 압니다!
                            .contentType(contentType != null ? contentType : "image/jpeg")
                            .build()
            );

            log.info(" MinIO 업로드 성공: {}", fileName);

            String baseUrl = minioUrl.endsWith("/") ? minioUrl : minioUrl + "/";
            return baseUrl + bucketName + "/" + fileName;

        } catch (Exception e) {
            log.error("URL 업로드 최종 실패: {} / 사유: {}", imageUrl, e.getMessage());
            throw new ExternalApiCallException("Mino에 url 저장하는 도중 예외 발생");
            // 실패 시 null 반환 -> DB에는 원본 URL 저장
        }

    }
}