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

    // 프론트 파일 업로드용 (원본 화질 그대로 저장)
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

            return bucketName + "/" + fileName;

        } catch (Exception e) {
            log.error("MinIO 이미지 업로드 실패", e);
            throw new ExternalApiCallException("이미지 업로드에 실패했습니다.");
        }
    }

    // URL 이미지 다운로드 및 업로드 (화질 개선 로직 적용)
    public String uploadFromUrl(String imageUrl) {
        try {
            // 알라딘 이미지 URL일 경우, 저화질 패턴을 고화질로 강제 변경
            if (imageUrl.contains("aladin.co.kr")) {
                // 썸네일 경로(/sum/)를 미리보기 경로(/letslook/)로 변경 (화질 향상)
                imageUrl = imageUrl.replace("/sum/", "/letslook/");

                // cover 뒤에 숫자가 붙거나 안 붙은 모든 경우(cover200, cover150, cover 등)를 cover500으로 변경
                // 정규식 설명: "cover" 뒤에 숫자(\d)가 0개 이상(*) 있는 부분을 "cover500"으로 치환
                imageUrl = imageUrl.replaceAll("cover\\d*", "cover500");
            }

            // 알라딘에서 이미지 다운로드 (메모리에 저장)
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 차단 우회 헤더
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.aladin.co.kr/");
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000); // 5초
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                // letslook(미리보기)이나 cover500이 없는 경우 원본 URL로 재시도 로직이 필요할 수 있으나,
                // 여기서는 예외 처리하여 로그를 남김
                log.warn("고화질 이미지 다운로드 실패, URL: {}", imageUrl);
                throw new ExternalApiCallException("외부 이미지 다운로드 실패 (응답 코드: " + responseCode + ")");
            }

            // 이미지를 byte 배열로 한 번에 읽어옴
            byte[] imageBytes;
            String contentType;
            try (InputStream is = conn.getInputStream()) {
                imageBytes = is.readAllBytes();
                contentType = conn.getContentType();
            }

            // MinIO로 업로드
            String extension = (contentType != null && contentType.contains("png")) ? ".png" : ".jpg";
            String fileName = UUID.randomUUID().toString() + extension;

            // 바이트 배열을 스트림으로 변환
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(bais, imageBytes.length, -1)
                            .contentType(contentType != null ? contentType : "image/jpeg")
                            .build()
            );

            log.info("MinIO 업로드 성공 (고화질 적용): {}", fileName);

            return bucketName + "/" + fileName;

        } catch (Exception e) {
            log.error("URL 업로드 최종 실패: {} / 사유: {}", imageUrl, e.getMessage());
            throw new ExternalApiCallException("MinIO에 URL 이미지를 저장하는 도중 예외 발생");
        }
    }
}