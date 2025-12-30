package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.service.strategy.BookEnrichStrategy;
import com.nhnacademy.book.service.strategy.BookSearchStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookAiRegistrationServiceTest {

    @Mock
    private BookSearchStrategy bookSearchStrategy;

    @Mock
    private BookEnrichStrategy bookEnrichStrategy;

    @InjectMocks
    private BookAiRegistrationService bookAiRegistrationService;

    @Test
    @DisplayName("도서 조회 후 추가 정보(Enrich)가 적용 가능한 경우 - Enrich된 객체 반환")
    void getBookInfoByIsbn_ShouldReturnEnrichedRequest_WhenApplicable() {
        // given
        String isbn = "9781234567890";
        BookCreateRequest initialRequest = createDummyRequest(isbn, "Original Title");
        BookCreateRequest enrichedRequest = createDummyRequest(isbn, "Enriched Title");

        // 검색 전략은 초기 객체를 반환
        given(bookSearchStrategy.searchBook(isbn)).willReturn(initialRequest);
        // 보강 전략(Enrich) 적용 가능하다고 설정
        given(bookEnrichStrategy.isApplicable(initialRequest)).willReturn(true);
        // 보강 전략 실행 시 수정된 객체 반환
        given(bookEnrichStrategy.enrich(initialRequest)).willReturn(enrichedRequest);

        // when
        BookCreateRequest result = bookAiRegistrationService.getBookInfoByIsbn(isbn);

        // then
        assertThat(result).isEqualTo(enrichedRequest); // 결과가 보강된 객체여야 함
        assertThat(result.bookName()).isEqualTo("Enriched Title");

        verify(bookEnrichStrategy).enrich(initialRequest); // enrich()가 호출되었는지 검증
    }

    @Test
    @DisplayName("도서 조회 후 추가 정보(Enrich)가 필요 없는 경우 - 원본 객체 반환")
    void getBookInfoByIsbn_ShouldReturnOriginalRequest_WhenNotApplicable() {
        // given
        String isbn = "9780987654321";
        BookCreateRequest initialRequest = createDummyRequest(isbn, "Original Title");

        // 검색 전략은 초기 객체를 반환
        given(bookSearchStrategy.searchBook(isbn)).willReturn(initialRequest);
        // 보강 전략 적용 불가능(false) 설정
        given(bookEnrichStrategy.isApplicable(initialRequest)).willReturn(false);

        // when
        BookCreateRequest result = bookAiRegistrationService.getBookInfoByIsbn(isbn);

        // then
        assertThat(result).isEqualTo(initialRequest); // 원본 객체가 그대로 나와야 함

        verify(bookEnrichStrategy, never()).enrich(any()); // enrich()가 절대 호출되면 안 됨
    }

    // 테스트용 더미 객체 생성
    private BookCreateRequest createDummyRequest(String isbn, String title) {
        return new BookCreateRequest(
                isbn,
                title,
                "Description",
                "Publisher",
                "Author",
                "tag1,tag2",
                Collections.emptyList(),
                LocalDate.now(),
                "Index",
                true,
                BookState.ON_SALE,
                100,
                20000,
                18000,
                "image.jpg"
        );
    }
}