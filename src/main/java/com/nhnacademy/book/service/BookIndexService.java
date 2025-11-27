package com.nhnacademy.book.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookIndexService {

    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

//    private static final String TTB_KEY = "ttbchoe26390950001";
//    private static final String TTB_KEY = "tttbchoe26390950002";
    private static final String TTB_KEY = "ttbchlwlgns06022139001";
    private static final String ALADIN_API_URL = "https://www.aladin.co.kr/ttb/api/ItemLookUp.aspx";
    private static final String DEFAULT_IMAGE_URL = "/images/default_book.png";

    @Transactional
    public int augmentBookIndexBatch(Pageable pageable) {

        // DB에서 도서 목록 조회
        List<Book> candidates = bookRepository.findAugmentationCandidates(DEFAULT_IMAGE_URL, pageable);

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
                    book.setBookIndex(toc); // JPA 변경 감지
                    updatedCount++;
                }
                // 중요! 목차가 없는 경우에도 처리를 해줘야 함 (무한 루프 방지)
                else {
                    book.setBookIndex("목차 정보 없음"); // 조회 쿼리 조건(NULL or '')을 피하기 위한 값
                    log.info("ISBN {} : 알라딘에 목차가 존재하지 않아 '목차 정보 없음'으로 처리", book.getIsbn());
                }

            } catch (Exception e) {
                // API 호출 실패 시, 로그만 남기고 다음 도서로 진행
                log.warn("ISBN {} 목차 증강 실패: {}", book.getIsbn(), e.getMessage());
            }
        }

        log.info("페이지 {} 처리 완료. 총 {}건 목차 업데이트.", pageable.getPageNumber(), updatedCount);
        return candidates.size();
    }

    private String fetchTableOfContents(String isbn) {
        // API 요청 URL 구성
        String url = UriComponentsBuilder.fromUriString(ALADIN_API_URL)
                .queryParam("ttbkey", TTB_KEY)
                .queryParam("itemId", isbn)
                .queryParam("itemIdType", "ISBN13")
                .queryParam("output", "js")
                .queryParam("Cover", "toc")
                .toUriString();

        // API 호출 및 응답
        String response = restTemplate.getForObject(url, String.class);

        // JSON 파싱
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode itemArray = root.path("item");

            if (itemArray.isArray() && itemArray.size() > 0) {
                JsonNode firstItem = itemArray.get(0);
                JsonNode bookInfo = firstItem.path("bookinfo"); // bookinfo 안으로 진입
                String toc = bookInfo.path("toc").asText();

                // 목차 정보가 알라딘에서 '없음'을 뜻하는 값(null, empty, '.')이 아닐 경우 반환
                if (toc != null && !toc.isBlank() && !toc.equals(".")) {
                    return cleanToc(toc);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 및 목차 추출 실패", e);
        }
        return null;
    }
    public String getTableOfContentsByIsbn(String isbn) {
        // API 호출 로직 재사용
        return fetchTableOfContents(isbn);
    }

    private String cleanToc(String rawToc) {
        String cleaned = rawToc;

        // <br> 태그는 줄바꿈으로 변환 (엔터 효과)
        cleaned = cleaned.replaceAll("(?i)<br\\s*/?>", "\n");

        // 모든 HTML 태그 제거 (<p>, <b>, <div> 등 전부 제거)
        // < 로 시작하고 > 로 끝나는 패턴 삭제
        cleaned = cleaned.replaceAll("<[^>]*>", "");

        // 유니코드 공백(&nbsp; 같은 것) 제거 (혹시 몰라서 추가)
        cleaned = cleaned.replace("\u00A0", " ");

        // 연속된 줄바꿈(\n\n\n)을 하나의 줄바꿈(\n)으로 압축
        cleaned = cleaned.replaceAll("\\n+", "\n");

        return cleaned.trim();
    }
}