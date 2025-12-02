package com.nhnacademy.book.service.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.entity.BookState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GeminiBookEnrichmentStrategyTest {
    private GeminiBookEnrichmentStrategy strategy;

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        strategy = new GeminiBookEnrichmentStrategy(restTemplate, objectMapper);
        ReflectionTestUtils.setField(strategy, "geminiApiKey", "test-api-key");
    }

    @Test
    @DisplayName("목차가 비어있을 경우 -> true")
    void isApplicable_whenIndexIsEmpty(){
        BookCreateRequest request = createRequestWith("", "정상적인 긴 설명입니다. 50자가 넘어가도록 길게 작성해야 AI가 보강을 안 하겠죠?");
        assertThat(strategy.isApplicable(request)).isTrue();
    }

    @Test
    @DisplayName("설명이 50자 이하인 경우 -> true")
    void isApplicable_ShortDescription() {
        BookCreateRequest request = createRequestWith("1. 목차있음", "짧은 설명");
        assertThat(strategy.isApplicable(request)).isTrue();
    }

    @Test
    @DisplayName("목차도 있고 설명도 충분한 경우 -> false")
    void isApplicable_False() {
        BookCreateRequest request = createRequestWith("1. 목차있음", "이 설명은 50자가 충분히 넘는 아주 훌륭하고 긴 설명입니다. 따라서 AI가 개입할 필요가 없습니다.");
        assertThat(strategy.isApplicable(request)).isFalse();
    }

    // --- 추가된 enrich 테스트 ---

    @Test
    @DisplayName("Gemini API 호출 성공 시: 응답을 파싱하여 설명과 목차를 보강한다")
    void enrich_Success() {
        // given
        BookCreateRequest original = createRequestWith("", "짧은 설명");

        // [수정 포인트]
        // 1. \\" -> \" (설명 안의 따옴표)
        // 2. \\\\n -> 줄바꿈 (목차 안의 줄바꿈은 4번 이스케이프 해야 함)
        String mockApiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\\n{ \\"description\\": \\"AI가 새로 작성한 멋진 설명입니다.\\", \\"index\\": \\"1장. AI 목차\\\\n2장. 결론\\" }\\n```"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        given(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .willReturn(mockApiResponse);

        // when
        BookCreateRequest result = strategy.enrich(original);

        // then
        assertThat(result.bookDescription()).isEqualTo("AI가 새로 작성한 멋진 설명입니다.");

        // 기대값도 줄바꿈 문자로 확인
        assertThat(result.bookIndex()).isEqualTo("1장. AI 목차\n2장. 결론");
    }

    @Test
    @DisplayName("Gemini API 호출 실패 시: 예외를 삼키고 원본 객체를 반환한다 (Fallback)")
    void enrich_ApiFailure() {
        // given
        BookCreateRequest original = createRequestWith("", "짧은 설명");

        // API 호출 시 예외 발생 설정
        given(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .willThrow(new RuntimeException("API 서버 오류"));

        // when
        BookCreateRequest result = strategy.enrich(original);

        // then
        // 원본과 동일한 객체인지 확인 (참조 혹은 값 비교)
        assertThat(result).isSameAs(original);
        assertThat(result.bookDescription()).isEqualTo("짧은 설명"); // 변경되지 않음
    }

    @Test
    @DisplayName("Gemini 응답 파싱 실패 시: 원본 객체를 반환한다")
    void enrich_ParsingFailure() {
        // given
        BookCreateRequest original = createRequestWith("", "짧은 설명");

        // JSON 형식이 잘못된 응답 (text 내부가 JSON이 아님)
        String mockInvalidResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "이건 JSON이 아니라 그냥 평문입니다."
                      }
                    ]
                  }
                }
              ]
            }
            """;

        given(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .willReturn(mockInvalidResponse);

        // when
        BookCreateRequest result = strategy.enrich(original);

        // then
        assertThat(result).isEqualTo(original);
    }

    private BookCreateRequest createRequestWith(String index, String description) {
        return new BookCreateRequest(
                "978-1234567890",
                "테스트 책 제목",
                description,
                "테스트 출판사",
                LocalDate.now(),
                index,
                true,
                BookState.ON_SALE,
                100,
                15000,
                13500,
                "test_image.jpg"
        );
    }

}