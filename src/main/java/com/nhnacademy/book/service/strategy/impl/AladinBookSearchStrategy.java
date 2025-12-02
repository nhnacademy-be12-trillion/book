package com.nhnacademy.book.service.strategy.impl;

import com.nhnacademy.book.aladin.AladinResponse;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.entity.BookState;
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

    @Value("${aladin.api.ttb-key}") //키 만료 된듯.
    private String ttbKey;

    @Override
    public BookCreateRequest searchBook(String isbn) {
        AladinResponse.Item aladinItem = fetchAladinData(isbn);
        return mapToDto(aladinItem);

    }

    //알라딘에 요청 http 보내고 가져온 데이터 mapToDto 로 바로 역직렬화.
    private AladinResponse.Item fetchAladinData(String isbn){
        URI uri = UriComponentsBuilder
                .fromHttpUrl("http://www.aladin.co.kr/ttb/api/ItemLookUp.aspx")
                .queryParam("ttbkey", ttbKey)
                .queryParam("itemIdType", "ISBN13")
                .queryParam("ItemId", isbn)
                .queryParam("Output", "JS")
                .queryParam("Version", "20131101")
                .queryParam("OptResult", "toc") // 목차 포함 요청
                .build()
                .toUri();

        try {
            // getForObject: 객체로 바로 매핑
            AladinResponse response = restTemplate.getForObject(uri, AladinResponse.class);

            if (response == null || response.item() == null || response.item().isEmpty()) {
                throw new IllegalArgumentException("알라딘에서 책을 찾을 수 없습니다: " + isbn);
            }
            return response.item().get(0);
        } catch (Exception e) {
            log.error("알라딘 API 호출 실패", e);
            throw new RuntimeException("도서 정보를 가져오는 중 오류가 발생했습니다.");
        }
    }


    //알라딘에서 가져온 데이터 -> BookCreateRequest로 변환.
    private BookCreateRequest mapToDto(AladinResponse.Item item) {
        LocalDate pubDate = LocalDate.parse(item.pubDate(), DateTimeFormatter.ISO_DATE);
        String index = (item.subInfo() != null && item.subInfo().toc() != null) ? item.subInfo().toc() : "";

        int noDiscountRate = item.priceStandard();

        return new BookCreateRequest(
                item.isbn(), item.title(), item.description(), item.publisher(),
                pubDate, index, true, BookState.ON_SALE, 0,
                item.priceStandard(), noDiscountRate, item.cover()
        );
    }
}
