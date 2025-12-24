package com.nhnacademy.book.service.strategy.impl;

import com.nhnacademy.book.aladin.AladinResponse;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.exception.BookNotFoundException;
import com.nhnacademy.book.exception.ExternalApiCallException;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.strategy.BookSearchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class AladinBookSearchStrategy implements BookSearchStrategy {
    private final RestTemplate restTemplate;
    private final BookService bookService; // 할인율 계산용
    private static final double DEFAULT_DISCOUNT_RATE = 10.0;

    @Value("${aladin.api.ttb-key}")
    private String ttbKey;

    @Override
    public BookCreateRequest searchBook(String isbn) {
        AladinResponse.Item aladinItem = fetchAladinData(isbn);
        return mapToDto(aladinItem);
    }

    private AladinResponse.Item fetchAladinData(String isbn) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.aladin.co.kr/ttb/api/ItemLookUp.aspx")
                .queryParam("ttbkey", ttbKey)
                .queryParam("itemIdType", "ISBN13")
                .queryParam("ItemId", isbn)
                .queryParam("Output", "JS")
                .queryParam("Version", "20131101")
                .queryParam("OptResult", "toc")
                .build()
                .toUri();

        try {
            AladinResponse response = restTemplate.getForObject(uri, AladinResponse.class);

            // 1. 결과가 없으면 우리가 만든 커스텀 예외 발생 (404용)
            if (response == null || response.item() == null || response.item().isEmpty()) {
                throw new BookNotFoundException(isbn);
            }
            return response.item().get(0);

        } catch (BookNotFoundException e) {
            // [핵심] 우리가 의도한 예외는 잡지 말고 통과시킴 -> GlobalExceptionHandler가 처리
            throw e;
        } catch (Exception e) {
            // [핵심] 그 외 예상치 못한 에러(타임아웃, 파싱 오류 등)는 로그 찍고 포장해서 던짐
            log.error("알라딘 API 호출 실패 - ISBN: {}", isbn, e);
            throw new ExternalApiCallException("외부 도서 API 연동 중 오류가 발생했습니다.");
        }
    }

    private BookCreateRequest mapToDto(AladinResponse.Item item) {
        LocalDate pubDate = LocalDate.parse(item.pubDate(), DateTimeFormatter.ISO_DATE);

        // [필수] 목차 HTML 태그 정리 (textarea 가독성용)
        String rawToc = (item.subInfo() != null && item.subInfo().toc() != null) ? item.subInfo().toc() : "";
        String cleanIndex = cleanToc(rawToc);

        int salePrice = bookService.calculateSalePrice(item.priceStandard(), DEFAULT_DISCOUNT_RATE);

        // 태그 추출 로직
        StringBuilder tagBuilder = new StringBuilder();
        if (item.categoryName() != null && !item.categoryName().isBlank()) {
            String[] parts = item.categoryName().split(">");
            for (String part : parts) {
                String tag = part.trim();
                if (!tag.isEmpty() && !tag.equals("국내도서") && !tag.equals("외국도서")) {
                    if (tagBuilder.length() > 0) tagBuilder.append(",");
                    tagBuilder.append(tag);
                }
            }
        }

        return new BookCreateRequest(
                item.isbn(), item.title(), item.description(), item.publisher(), item.author(),
                tagBuilder.toString(), null, pubDate,
                cleanIndex, // 정제된 목차 사용
                true, BookState.ON_SALE, 0,
                item.priceStandard(), salePrice, item.cover()
        );
    }

    // [필수 로직] 관리자가 <br> 태그를 보고 편집할 순 없으므로 변환 필요
    private String cleanToc(String rawToc) {
        if (rawToc == null || rawToc.isBlank()) return "";

        // 1. <br> 태그를 줄바꿈(\n)으로 변환 (이게 제일 중요)
        String cleaned = rawToc.replaceAll("(?i)<br\\s*/?>", "\n");

        // 2. 나머지 잡다한 HTML 태그 제거 (<b>, <span> 등)
        cleaned = cleaned.replaceAll("<[^>]*>", "");

        // 3. HTML 특수문자(&nbsp;) 공백 처리
        cleaned = cleaned.replace("&nbsp;", " ").replace("\u00A0", " ");

        return cleaned.trim();
    }
}