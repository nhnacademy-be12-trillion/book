package com.nhnacademy.book.service.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.entity.BookState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiBookEnrichmentStrategyTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GeminiBookEnrichmentStrategy strategy;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(strategy, "geminiApiKey", "test-api-key");
    }

    @Test
    @DisplayName("isApplicable: 목차가 비어있으면 true 반환")
    void isApplicable_EmptyIndex() {
        BookCreateRequest request = createBookRequest("충분히 긴 설명입니다. ".repeat(5), "");
        assertThat(strategy.isApplicable(request)).isTrue();
    }

    @Test
    @DisplayName("isApplicable: 설명이 50자 미만이면 true 반환")
    void isApplicable_ShortDescription() {
        BookCreateRequest request = createBookRequest("짧은 설명", "1장. 목차");
        assertThat(strategy.isApplicable(request)).isTrue();
    }

    @Test
    @DisplayName("isApplicable: 목차도 있고 설명도 충분하면 false 반환")
    void isApplicable_NotApplicable() {
        BookCreateRequest request = createBookRequest("충분히 긴 설명입니다. ".repeat(5), "1장. 목차");
        assertThat(strategy.isApplicable(request)).isFalse();
    }

    @Test
    @DisplayName("enrich: Gemini API 호출 성공 및 데이터 보강 확인 (마크다운 포함, 이중 이스케이프 처리)")
    void enrich_Success() {
        // given
        BookCreateRequest original = createBookRequest("짧은 설명", "");

        // [중요] JSON 문자열 안에 또 JSON이 들어가는 구조입니다.
        // 내부 JSON의 "index" 값에 있는 줄바꿈이 파싱 과정에서 살아남으려면
        // Java 코드상에서 역슬래시를 4개(\\\\n) 써야 합니다.
        // 1. Java 컴파일러: \\\\n -> \\n (문자열)
        // 2. 겉 포장 JSON 파싱: \\n -> \n (문자열)
        // 3. 내부 JSON 파싱: \n -> 줄바꿈 문자 (정상 값)
        String mockApiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\\n{\\n  \\"description\\": \\"AI가 생성한 멋진 설명\\",\\n  \\"index\\": \\"1장. 서론\\\\n2장. 본론\\"\\n}\\n```"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn(mockApiResponse);

        // when
        BookCreateRequest enriched = strategy.enrich(original);

        // then
        assertThat(enriched.bookDescription()).isEqualTo("AI가 생성한 멋진 설명");
        // 실제 결과값에는 정상적인 줄바꿈 문자가 들어있어야 함
        assertThat(enriched.bookIndex()).isEqualTo("1장. 서론\n2장. 본론");

        // 변경되지 말아야 할 필드 확인
        assertThat(enriched.bookName()).isEqualTo(original.bookName());
        assertThat(enriched.isbn()).isEqualTo(original.isbn());
    }

    @Test
    @DisplayName("enrich: 429 Too Many Requests 에러 발생 시 원본 반환")
    void enrich_TooManyRequests() {
        // given
        BookCreateRequest original = createBookRequest("짧은 설명", "");

        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        // when
        BookCreateRequest result = strategy.enrich(original);

        // then
        assertThat(result).isEqualTo(original);
    }

    @Test
    @DisplayName("enrich: 일반 예외 발생 시 원본 반환")
    void enrich_GeneralException() {
        // given
        BookCreateRequest original = createBookRequest("짧은 설명", "");

        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API 서버 다운"));

        // when
        BookCreateRequest result = strategy.enrich(original);

        // then
        assertThat(result).isEqualTo(original);
    }

    @Test
    @DisplayName("enrich: API 응답이 왔지만 JSON 파싱 실패 시 원본 반환")
    void enrich_ParsingError() {
        // given
        BookCreateRequest original = createBookRequest("짧은 설명", "");

        // text 필드 내용이 유효한 JSON이 아닌 경우
        String invalidJsonResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "이것은 JSON이 아닙니다." }
                    ]
                  }
                }
              ]
            }
            """;

        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn(invalidJsonResponse);

        // when
        BookCreateRequest result = strategy.enrich(original);

        // then
        // 로그에 파싱 에러가 찍히고 원본이 반환되어야 함
        assertThat(result).isEqualTo(original);
    }

    private BookCreateRequest createBookRequest(String description, String index) {
        return new BookCreateRequest(
                "1234567890",
                "테스트 책",
                description,
                "테스트 출판사",
                "테스트 저자",
                "태그",
                List.of(1L),
                LocalDate.now(),
                index,
                true,
                BookState.ON_SALE,
                100,
                10000,
                9000,
                "image.jpg"
        );
    }
}