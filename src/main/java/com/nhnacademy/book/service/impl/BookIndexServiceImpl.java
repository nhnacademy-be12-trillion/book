package com.nhnacademy.book.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.service.BookIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookIndexServiceImpl implements BookIndexService {

    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${aladin.api.ttb-key}")
    private String ttbKey;

    @Value("${aladin.api.url}")
    private String aladinApiUrl;

    @Value("${book.default-image}")
    private String defaultImageUrl;
    @Override
    @Transactional
    public int augmentBookIndexBatch(Pageable pageable) {

        // DB에서 목차가 없는 도서 목록 조회
        List<Book> candidates = bookRepository.findAugmentationCandidates(defaultImageUrl, pageable);

        if (candidates.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (Book book : candidates) {
            try {
                // 외부 API 호출 및 목차 추출
                String toc = fetchTableOfContents(book.getIsbn());

                // 목차 데이터가 유효하다면 DB 업데이트
                if (toc != null && !toc.isBlank()) {
                    book.setBookIndex(toc); // JPA 변경 감지(Dirty Checking)로 자동 저장
                    updatedCount++;
                }
                // 중요! 목차가 없는 경우에도 처리를 해줘야 함 (무한 루프 방지)
                else {
                    book.setBookIndex("목차 정보 없음"); // 조회 쿼리 조건(NULL or '')을 피하기 위한 값
                    log.info("ISBN {} : 알라딘에 목차가 존재하지 않아 '목차 정보 없음'으로 처리", book.getIsbn());
                }

            } catch (Exception e) {
                // API 호출 실패 시, 로그만 남기고 다음 도서로 진행 (전체 로직 중단 방지)
                log.warn("ISBN {} 목차 증강 실패: {}", book.getIsbn(), e.getMessage());
            }
        }

        log.info("페이지 {} 처리 완료. 총 {}건 목차 업데이트.", pageable.getPageNumber(), updatedCount);
        return candidates.size();
    }

    @Override
    public String getTableOfContentsByIsbn(String isbn) {
        // 내부 private 메서드 재사용
        return fetchTableOfContents(isbn);
    }


    private String fetchTableOfContents(String isbn) {
        // API 요청 URL 구성
        String url = UriComponentsBuilder.fromUriString(aladinApiUrl)
                .queryParam("ttbkey", ttbKey)
                .queryParam("itemId", isbn)
                .queryParam("itemIdType", "ISBN13")
                .queryParam("output", "js")
                .queryParam("Cover", "toc") // 목차 정보 요청
                .toUriString();

        try {
            // API 호출
            String response = restTemplate.getForObject(url, String.class);

            // JSON 파싱
            JsonNode root = objectMapper.readTree(response);
            JsonNode itemArray = root.path("item");

            if (itemArray.isArray() && itemArray.size() > 0) {
                JsonNode firstItem = itemArray.get(0);
                JsonNode bookInfo = firstItem.path("bookinfo"); // bookinfo 노드 진입
                String toc = bookInfo.path("toc").asText();

                // 목차 정보가 유효한지 체크 (알라딘은 없을 때 빈 값이나 '.'을 주기도 함)
                if (toc != null && !toc.isBlank() && !toc.equals(".")) {
                    return cleanToc(toc);
                }
            }
        } catch (Exception e) {
            // 개별 도서 실패 시 null 반환하여 상위 메서드에서 처리하도록 함
            log.warn("알라딘 API 파싱 실패 (ISBN: {}): {}", isbn, e.getMessage());
            return null;
        }
        return null;
    }

    private String cleanToc(String rawToc) {
        String cleaned = rawToc;

        // <br> 태그는 줄바꿈으로 변환
        cleaned = cleaned.replaceAll("(?i)<br\\s*/?>", "\n");

        // 모든 HTML 태그 제거 (<p>, <b> 등)
        cleaned = cleaned.replaceAll("<[^>]*>", "");

        // 유니코드 공백(&nbsp; 등) 제거
        cleaned = cleaned.replace("\u00A0", " ");

        // 연속된 줄바꿈을 하나로 압축
        cleaned = cleaned.replaceAll("\\n+", "\n");

        return cleaned.trim();
    }
}