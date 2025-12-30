package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.exception.ExternalApiCallException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @InjectMocks
    private MinioService minioService;

    @Mock
    private MinioClient minioClient;

    private final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        // @Value("${minio.bucket}") 값을 주입하기 위해 ReflectionTestUtils 사용
        ReflectionTestUtils.setField(minioService, "bucketName", BUCKET_NAME);
    }

    @Test
    @DisplayName("이미지 파일 업로드 성공")
    void uploadImage_Success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                "test data".getBytes()
        );

        // when
        String result = minioService.uploadImage(file);

        // then
        assertThat(result).startsWith(BUCKET_NAME + "/");
        assertThat(result).endsWith(".jpg");

        // MinioClient가 실제로 호출되었는지 검증
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("이미지 파일 업로드 실패 - MinIO 예외 발생")
    void uploadImage_Fail_MinioException() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        // MinIO 호출 시 예외 발생하도록 설정
        willThrow(new RuntimeException("MinIO Error"))
                .given(minioClient).putObject(any(PutObjectArgs.class));

        // when & then
        assertThatThrownBy(() -> minioService.uploadImage(file))
                .isInstanceOf(ExternalApiCallException.class)
                .hasMessage("이미지 업로드에 실패했습니다.");
    }

    @Test
    @DisplayName("이미지 파일 업로드 실패 - 파일이 비어있음")
    void uploadImage_Fail_EmptyFile() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // when
        String result = minioService.uploadImage(emptyFile);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("이미지 파일 업로드 실패 - 파일이 Null")
    void uploadImage_Fail_NullFile() {
        // when
        String result = minioService.uploadImage(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("URL로 이미지 업로드 실패 - 잘못된 URL")
    void uploadFromUrl_Fail_InvalidUrl() {
        // given
        String invalidUrl = "http://invalid-url.com/image.jpg";

        // 실제 네트워크 연결을 시도하다가 Exception이 터지고,
        // catch 블록에서 ExternalApiCallException을 던지는지 확인

        // when & then
        assertThatThrownBy(() -> minioService.uploadFromUrl(invalidUrl))
                .isInstanceOf(ExternalApiCallException.class)
                .hasMessage("Mino에 url 저장하는 도중 예외 발생");
    }

}