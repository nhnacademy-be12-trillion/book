package com.nhnacademy.book.service.strategy.impl;

import com.nhnacademy.book.aladin.AladinResponse;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.exception.BookNotFoundException;
import com.nhnacademy.book.exception.ExternalApiCallException;
import com.nhnacademy.book.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AladinBookSearchStrategyTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private BookService bookService;

    @InjectMocks
    private AladinBookSearchStrategy strategy;

    private final String TEST_TTB_KEY = "test-ttb-key";

    @BeforeEach
    void setUp() {
        // @Value("${aladin.api.ttb-key}") 필드에 리플렉션으로 값 주입
        ReflectionTestUtils.setField(strategy, "ttbKey", TEST_TTB_KEY);
    }

    @Test
    @DisplayName("searchBook: 정상적으로 도서 정보를 가져와 DTO로 변환한다.")
    void searchBook_Success() {
        // given
        String isbn = "9788912345678";

        // Mock 데이터 생성
        AladinResponse.SubInfo subInfo = new AladinResponse.SubInfo(
                "Chapter 1<br>Chapter 2<b>Bold</b>&nbsp;", // 정제 필요한 목차
                "부제목",
                "원제"
        );

        AladinResponse.Item item = new AladinResponse.Item(
                "테스트 책 제목",
                "테스트 작가",
                "2023-10-01", // ISO_DATE 형식
                "책 설명입니다.",
                isbn,
                20000,
                "cover.jpg",
                "NHN출판",
                "국내도서>IT 모바일>Java", // 카테고리 파싱 테스트용
                subInfo
        );

        AladinResponse aladinResponse = new AladinResponse(List.of(item));

        // RestTemplate 동작 정의
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(AladinResponse.class)
        )).thenReturn(new ResponseEntity<>(aladinResponse, HttpStatus.OK));

        // BookService (할인율 계산) 동작 정의
        when(bookService.calculateSalePrice(anyInt(), anyDouble())).thenReturn(18000);

        // when
        BookCreateRequest result = strategy.searchBook(isbn);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isbn()).isEqualTo(isbn);
        assertThat(result.bookName()).isEqualTo("테스트 책 제목");
        assertThat(result.bookRegularPrice()).isEqualTo(20000);
        assertThat(result.bookSalePrice()).isEqualTo(18000); // Mock된 서비스 결과 확인

        // 태그 파싱 확인 ("국내도서" 제외, "IT 모바일", "Java" 포함)
        assertThat(result.tags()).contains("IT 모바일", "Java");
        assertThat(result.tags()).doesNotContain("국내도서");

        // 목차 정제 로직 확인 (<br> -> \n, <b> 제거, &nbsp; -> 공백)
        assertThat(result.bookIndex()).isEqualTo("Chapter 1\nChapter 2Bold");

        // 상태값 기본 설정 확인
        assertThat(result.bookState()).isEqualTo(BookState.ON_SALE);
        assertThat(result.bookPackaging()).isTrue();
    }

    @Test
    @DisplayName("searchBook: 검색 결과가 없을 경우 BookNotFoundException 발생")
    void searchBook_NotFound_EmptyList() {
        // given
        String isbn = "9788900000000";

        // 아이템 리스트가 비어있는 응답
        AladinResponse emptyResponse = new AladinResponse(Collections.emptyList());

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(AladinResponse.class)
        )).thenReturn(new ResponseEntity<>(emptyResponse, HttpStatus.OK));

        // when & then
        assertThatThrownBy(() -> strategy.searchBook(isbn))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining(isbn);
    }

    @Test
    @DisplayName("searchBook: API 응답이 null일 경우 BookNotFoundException 발생")
    void searchBook_NotFound_NullBody() {
        // given
        String isbn = "9788900000000";

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(AladinResponse.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // when & then
        assertThatThrownBy(() -> strategy.searchBook(isbn))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("searchBook: 외부 API 호출 중 예외 발생 시 ExternalApiCallException으로 감싸서 던짐")
    void searchBook_ExternalApiError() {
        // given
        String isbn = "9788912345678";

        // RestTemplate이 RuntimeException을 던지도록 설정
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(AladinResponse.class)
        )).thenThrow(new RuntimeException("Connection Timeout"));

        // when & then
        assertThatThrownBy(() -> strategy.searchBook(isbn))
                .isInstanceOf(ExternalApiCallException.class)
                .hasMessage("외부 도서 API 연동 중 오류가 발생했습니다.");
    }
}