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
                // 외부 API 호출 (예외 발생 시 catch 블록으로 이동하여 건너뜀)
                String toc = fetchTableOfContents(book.getIsbn());

                // 목차 데이터가 유효하다면 DB 업데이트
                if (toc != null && !toc.isBlank()) {
                    book.setBookIndex(toc);
                    updatedCount++;
                }
                // 목차가 빈 값으로 온 경우 (API는 성공했으나 내용이 없음)
                else {
                    book.setBookIndex("목차 정보 없음");
                    log.info("ISBN {} : 알라딘에 목차가 존재하지 않아 '목차 정보 없음'으로 처리", book.getIsbn());
                }

            } catch (Exception e) {
                // API 호출 실패(네트워크 오류, 파싱 오류 등) 시
                // DB를 업데이트하지 않고 로그만 남기고 다음 도서로 진행 (중요!)
                log.warn("ISBN {} 목차 증강 실패: {}", book.getIsbn(), e.getMessage());
            }
        }

        // [수정] pageable.unpaged() 일 때 getPageNumber() 호출 시 에러 방지
        int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
        log.info("페이지 {} 처리 완료. 총 {}건 목차 업데이트.", pageNumber, updatedCount);

        return candidates.size();
    }

    @Override
    public String getTableOfContentsByIsbn(String isbn) {
        try {
            return fetchTableOfContents(isbn);
        } catch (Exception e) {
            log.warn("목차 단건 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    // [수정] 예외를 내부에서 먹지 않고 밖으로 던짐 (throws Exception)
    private String fetchTableOfContents(String isbn) throws Exception {
        String url = UriComponentsBuilder.fromUriString(aladinApiUrl)
                .queryParam("ttbkey", ttbKey)
                .queryParam("itemId", isbn)
                .queryParam("itemIdType", "ISBN13")
                .queryParam("output", "js")
                .queryParam("Cover", "toc")
                .toUriString();

        // 여기서 예외 터지면 호출자(augmentBookIndexBatch)가 처리함
        String response = restTemplate.getForObject(url, String.class);

        JsonNode root = objectMapper.readTree(response);
        JsonNode itemArray = root.path("item");

        if (itemArray.isArray() && itemArray.size() > 0) {
            JsonNode firstItem = itemArray.get(0);
            JsonNode bookInfo = firstItem.path("bookinfo");
            String toc = bookInfo.path("toc").asText();

            if (toc != null && !toc.isBlank() && !toc.equals(".")) {
                return cleanToc(toc);
            }
        }
        return null; // 목차가 없거나 item이 비어있으면 null 반환
    }

    private String cleanToc(String rawToc) {
        String cleaned = rawToc;

        // <br> 태그는 줄바꿈으로 변환
        cleaned = cleaned.replaceAll("(?i)<br\\s*/?>", "\n");

        // 모든 HTML 태그 제거
        cleaned = cleaned.replaceAll("<[^>]*>", "");

        // 유니코드 공백(\u00A0) 제거
        cleaned = cleaned.replace("\u00A0", " ");

        // [추가] HTML 엔티티 공백(&nbsp;) 제거 -> 이게 없어서 테스트 실패했음
        cleaned = cleaned.replace("&nbsp;", " ");

        // 연속된 줄바꿈을 하나로 압축
        cleaned = cleaned.replaceAll("\\n+", "\n");

        return cleaned.trim();
    }
}