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


    //정보 보강 여부 -> 검색 api에서 가져온 인덱스가 비어있거나 설명이 50글자 이내.
    @Override
    public boolean isApplicable(BookCreateRequest request) {
        return request.bookIndex().isEmpty() || request.bookDescription().length()<50;
    }

    @Override
    public BookCreateRequest enrich(BookCreateRequest request) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 바디 설정 (Gemini API 스펙에 맞춘 Map 구조)
        // 구조: { "contents": [{ "parts": [{ "text": "프롬프트..." }] }] }
        Map<String, Object> requestBody = new HashMap<>();
        String prompt = createPrompt(request);

        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", prompt)
                ))
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 3. POST 요청
            String rawJsonParams = restTemplate.postForObject(url, entity, String.class);

            // 4. 응답 파싱 및 적용
            return applyGeminiResponse(request, rawJsonParams);

        } catch (Exception e) {
            log.error("Gemini 호출 실패 - 원본 데이터만 반환합니다.", e);
            return request;  // AI 실패 시 원본 그대로 리턴 (장애 전파 방지)
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
        """, book.bookName(), "", book.bookPublisher());
        // *주의: 알라딘 Item에 author가 있다면 그걸 넣는게 제일 좋습니다.
    }

    private BookCreateRequest applyGeminiResponse(BookCreateRequest original, String rawResponse) {
        try {
            // 1. 구글 API 응답에서 'text' 추출
            JsonNode root = objectMapper.readTree(rawResponse);
            String innerJsonText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // 코드블럭(```json ... ```)이 섞여 올 경우 제거
            innerJsonText = innerJsonText.replaceAll("```json", "").replaceAll("```", "").trim();

            // 2. AI가 만든 JSON을 다시 파싱
            JsonNode aiData = objectMapper.readTree(innerJsonText);
            String newDescription = aiData.path("description").asText(original.bookDescription());
            String newIndex = aiData.path("index").asText(original.bookIndex());

            // 3. 최종 병합
            return new BookCreateRequest(
                    original.isbn(), original.bookName(),
                    newDescription, // AI 설명 적용
                    original.bookPublisher(), original.bookPublicationDate(),
                    newIndex,       // AI 목차 적용
                    original.bookPackaging(), original.bookState(), original.bookStock(),
                    original.bookRegularPrice(), original.bookSalePrice(), original.bookImage()
            );
        } catch (Exception e) {
            log.warn("Gemini 응답 파싱 실패", e);
            return original;
        }
    }
}
