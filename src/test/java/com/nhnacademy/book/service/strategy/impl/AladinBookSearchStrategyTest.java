package com.nhnacademy.book.service.strategy.impl;

import com.nhnacademy.book.aladin.AladinResponse;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AladinBookSearchStrategyTest {

    @InjectMocks
    private AladinBookSearchStrategy strategy;

    @Mock
    RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(strategy,"ttbKey","test-key");
    }

    @Test
    @DisplayName("알라딘 API로 책 정보 조회 성공")
    void searchBook_Success(){
        String isbn = "1234567890123";

        AladinResponse.SubInfo subInfo = new AladinResponse.SubInfo("목차입니다", null, null);
        AladinResponse.Item item = new AladinResponse.Item(
                "테스트 책", "작가", "2023-10-01", "설명", isbn, 10000, "cover.jpg", "출판사", subInfo
        );
        AladinResponse response = new AladinResponse(List.of(item));

        given(restTemplate.getForObject(any(URI.class),eq(AladinResponse.class))).willReturn(response);

        BookCreateRequest result = strategy.searchBook(isbn);

        assertThat(result.isbn()).isEqualTo(isbn);
        assertThat(result.bookName()).isEqualTo("테스트 책");
        assertThat(result.bookIndex()).isEqualTo("목차입니다");
        assertThat(result.bookPublicationDate()).isEqualTo(java.time.LocalDate.parse("2023-10-01"));
    }

    @Test
    @DisplayName("알라딘에서 책을 찾을 수 없을 때 -> 예외")
    void bookSearch_Failure(){
        String isbn = "non-exists";
        AladinResponse emptyResponse = new AladinResponse(List.of());

        given(restTemplate.getForObject(any(URI.class),eq(AladinResponse.class))).willReturn(emptyResponse);

        assertThatThrownBy(() -> strategy.searchBook(isbn))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("도서 정보를 가져오는 중 오류가 발생했습니다");
    }

}