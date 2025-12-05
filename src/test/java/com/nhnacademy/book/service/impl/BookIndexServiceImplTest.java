package com.nhnacademy.book.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookIndexServiceImplTest {

    @InjectMocks
    private BookIndexServiceImpl bookIndexService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RestTemplate restTemplate;

    // ObjectMapper는 로직이 복잡하지 않으므로 Mock보다는 Spy나 실제 객체를 사용하는 것이 테스트하기 편합니다.
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // @Value 필드에 값을 주입 (Spring Context가 없는 Unit Test이므로 Reflection 사용)
        ReflectionTestUtils.setField(bookIndexService, "ttbKey", "test-ttb-key");
        ReflectionTestUtils.setField(bookIndexService, "aladinApiUrl", "http://api.aladin.co.kr");
        ReflectionTestUtils.setField(bookIndexService, "defaultImageUrl", "http://default.image");
    }

    @Test
    @DisplayName("목차 증강 배치 - 대상 도서가 없을 경우 0 반환")
    void augmentBookIndexBatch_NoCandidates() {
        // given
        Pageable pageable = Pageable.unpaged();
        given(bookRepository.findAugmentationCandidates(anyString(), any(Pageable.class)))
                .willReturn(Collections.emptyList());

        // when
        int result = bookIndexService.augmentBookIndexBatch(pageable);

        // then
        assertThat(result).isEqualTo(0);
        verify(restTemplate, times(0)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("목차 증강 배치 - 성공적으로 목차를 가져와서 업데이트함")
    void augmentBookIndexBatch_Success() {
        // given
        String isbn = "9781234567890";
        Book book = new Book();
        book.setIsbn(isbn);
        book.setBookIndex(null);

        Pageable pageable = Pageable.unpaged();
        given(bookRepository.findAugmentationCandidates(anyString(), any(Pageable.class)))
                .willReturn(List.of(book));

        // 알라딘 API 응답 JSON 모의 (Mock)
        String mockJsonResponse = """
                {
                    "item": [
                        {
                            "bookinfo": {
                                "toc": "1. 서론<br>2. 본론<b>중요</b>"
                            }
                        }
                    ]
                }
                """;
        given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(mockJsonResponse);

        // when
        int count = bookIndexService.augmentBookIndexBatch(pageable);

        // then
        assertThat(count).isEqualTo(1);
        // HTML 태그 제거 및 정제 로직 확인 ("<br>" -> "\n", "<b>" 제거)
        assertThat(book.getBookIndex()).contains("1. 서론");
        assertThat(book.getBookIndex()).contains("2. 본론중요");
    }

    @Test
    @DisplayName("목차 증강 배치 - API 응답에 목차가 비어있으면 '목차 정보 없음' 처리")
    void augmentBookIndexBatch_EmptyToc() {
        // given
        Book book = new Book();
        book.setIsbn("12345");

        given(bookRepository.findAugmentationCandidates(anyString(), any(Pageable.class)))
                .willReturn(List.of(book));

        // 목차가 없는 JSON 응답
        String mockJsonResponse = "{ \"item\": [ { \"bookinfo\": { \"toc\": \"\" } } ] }";
        given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(mockJsonResponse);

        // when
        bookIndexService.augmentBookIndexBatch(Pageable.unpaged());

        // then
        assertThat(book.getBookIndex()).isEqualTo("목차 정보 없음");
    }

    @Test
    @DisplayName("목차 증강 배치 - API 호출 중 예외 발생 시 해당 도서는 건너뛰고 진행")
    void augmentBookIndexBatch_ApiError() {
        // given
        Book book1 = new Book(); book1.setIsbn("error-isbn");
        Book book2 = new Book(); book2.setIsbn("normal-isbn"); // 얘는 성공한다고 가정

        given(bookRepository.findAugmentationCandidates(anyString(), any(Pageable.class)))
                .willReturn(List.of(book1, book2));

        // 첫 번째 책은 예외 발생
        given(restTemplate.getForObject(contains("error-isbn"), eq(String.class)))
                .willThrow(new RuntimeException("API Connection Refused"));

        // 두 번째 책은 정상 응답
        String validResponse = "{ \"item\": [ { \"bookinfo\": { \"toc\": \"Valid TOC\" } } ] }";
        given(restTemplate.getForObject(contains("normal-isbn"), eq(String.class)))
                .willReturn(validResponse);

        // when
        int count = bookIndexService.augmentBookIndexBatch(Pageable.unpaged());

        // then
        // candidates.size()를 반환하도록 되어 있으므로 2가 반환됨 (예외가 발생해도 로직이 멈추지 않음을 확인)
        assertThat(count).isEqualTo(2);

        // book1은 업데이트 안 됨
        assertThat(book1.getBookIndex()).isNull();
        // book2는 업데이트 됨
        assertThat(book2.getBookIndex()).isEqualTo("Valid TOC");
    }

    @Test
    @DisplayName("ISBN으로 목차 단건 조회 - 정상 반환")
    void getTableOfContentsByIsbn_Success() {
        // given
        String isbn = "999999";
        String mockResponse = """
                {
                    "item": [
                        {
                            "bookinfo": {
                                "toc": "Chapter 1.&nbsp;Start"
                            }
                        }
                    ]
                }
                """;
        given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(mockResponse);

        // when
        String result = bookIndexService.getTableOfContentsByIsbn(isbn);

        // then
        // &nbsp; 가 공백으로 치환되었는지 확인
        assertThat(result).isEqualTo("Chapter 1. Start");
    }

    @Test
    @DisplayName("ISBN으로 목차 단건 조회 - 알라딘 API 형식이 아닐 때 null 반환")
    void getTableOfContentsByIsbn_InvalidJson() {
        // given
        String isbn = "bad-response";
        // 이상한 JSON 응답 (item 배열이 없음)
        String mockResponse = "{ \"error\": \"not found\" }";
        given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(mockResponse);

        // when
        String result = bookIndexService.getTableOfContentsByIsbn(isbn);

        // then
        assertThat(result).isNull();
    }
}