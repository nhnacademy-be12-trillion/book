package com.nhnacademy.book.service.strategy.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.service.strategy.BookEnrichStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiBookEnrichmentStrategy implements BookEnrichStrategy {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.gemini-key}")
    private String geminiApiKey;

    // 정보 보강 여부 -> 검색 api에서 가져온 인덱스가 비어있거나 설명이 50글자 이내일 때만 AI 호출
    @Override
    public boolean isApplicable(BookCreateRequest request) {
        // 이미 내용이 충분하다면 굳이 AI를 부르지 않아서 한도를 아낍니다.
        return request.bookIndex().isEmpty() || request.bookDescription().length() < 50;
    }

    @Override
    public BookCreateRequest enrich(BookCreateRequest request) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 바디 설정
        Map<String, Object> requestBody = new HashMap<>();
        String prompt = createPrompt(request);

        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", prompt)
                ))
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // POST 요청
            String rawJsonParams = restTemplate.postForObject(url, entity, String.class);

            // 응답 파싱 및 적용
            return applyGeminiResponse(request, rawJsonParams);

        } catch (HttpClientErrorException.TooManyRequests e) {
            // 429 에러(한도 초과)는 시스템 장애가 아님 -> 경고 로그만 남기고 원본 반환
            log.warn("Gemini API 호출 한도 초과 (429): AI 설명을 건너뛰고 도서 등록을 진행합니다. (잠시 후 다시 시도하세요)");
            return request; // 원본 데이터 그대로 리턴 -> 도서 등록 성공

        } catch (Exception e) {
            // 그 외 알 수 없는 에러는 에러 로그를 남기고 원본 반환
            log.error("Gemini API 호출 중 예상치 못한 오류 발생 - 원본 데이터로 진행합니다.", e);
            return request;
        }
    }

    private String createPrompt(BookCreateRequest book) {
        return String.format("""
        책 제목: %s
        저자: %s
        출판사: %s
        
        [지시사항]
        위 책의 '상세 설명(description)'과 '목차(index)' 데이터가 필요해.
        
        1. description: 이 책을 개발자나 독자가 읽어야 하는 이유를 포함한 300자 내외의 매력적인 소개글.
        2. index: 이 책의 실제 목차를 알려줘.
           **중요: 만약 실제 목차를 모른다면, 책 제목과 주제를 바탕으로 그럴듯한 목차 5~10개 챕터를 반드시 생성해서 채워넣어.** (절대 비워두지 마)
        
        [출력 형식]
        반드시 아래 JSON 포맷만 출력해. (마크다운, 설명, 인사말 금지)
        {
          "description": "생성된 설명...",
          "index": "1장. 서론\\n2장. ..."
        }
        """, book.bookName(), book.bookAuthor(), book.bookPublisher());
    }

    private BookCreateRequest applyGeminiResponse(BookCreateRequest original, String rawResponse) {
        try {
            // 1. 구글 API 응답에서 'text' 추출
            JsonNode root = objectMapper.readTree(rawResponse);

            // 응답 구조가 예상과 다를 경우를 대비한 안전 장치
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                log.warn("Gemini 응답에 candidates가 없습니다.");
                return original;
            }

            String innerJsonText = candidates.get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // 코드블럭(```json ... ```) 제거
            innerJsonText = innerJsonText.replaceAll("```json", "").replaceAll("```", "").trim();

            // 2. AI가 만든 JSON을 다시 파싱
            JsonNode aiData = objectMapper.readTree(innerJsonText);
            String newDescription = aiData.path("description").asText(original.bookDescription());
            String newIndex = aiData.path("index").asText(original.bookIndex());

            // 3. 최종 병합 (기존 isPackaging, bookState 유지)
            return new BookCreateRequest(
                    original.isbn(), original.bookName(),
                    newDescription, // AI 설명 적용
                    original.bookPublisher(), original.bookAuthor(), original.tags(), original.categoryIdList(), original.bookPublicationDate(),
                    newIndex,       // AI 목차 적용
                    original.bookPackaging(), original.bookState(), original.bookStock(),
                    original.bookRegularPrice(), original.bookSalePrice(), original.bookImage()
            );
        } catch (Exception e) {
            log.warn("Gemini 응답 데이터 파싱 실패 - 원본 데이터 사용", e);
            return original;
        }
    }
}