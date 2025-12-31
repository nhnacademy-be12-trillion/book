package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.service.WishlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class) // Controller만 로드하여 빠르게 테스트
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가상의 HTTP 요청을 보내는 객체

    @MockitoBean
    private WishlistService wishlistService; // 서비스는 가짜 객체(Mock)로 대체

    @Test
    @DisplayName("위시리스트 조회 성공 테스트")
    void getWishlists_Success() throws Exception {
        // Given (준비)
        Long memberId = 1L;
        // 테스트용 빈 리스트 반환 설정 (실제 DTO 구조에 맞춰 데이터 채워도 됨)
        List<BookListResponse> mockResponse = List.of();

        // 서비스가 호출되면 가짜 데이터를 반환하도록 설정 (Stubbing)
        given(wishlistService.getWishlist(memberId)).willReturn(mockResponse);

        // When & Then (실행 및 검증)
        mockMvc.perform(get("/books/wishlists")
                        .header("X-Member-Id", memberId) // 헤더 필수
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 200 OK 확인
                .andExpect(jsonPath("$").isArray()) // 응답이 배열인지 확인
                .andDo(print()); // 로그 출력

        // Verify (행위 검증): 서비스가 실제로 1번 호출되었는지 확인
        verify(wishlistService).getWishlist(memberId);
    }

    @Test
    @DisplayName("위시리스트 토글 - 찜 목록에 추가(True) 테스트")
    void toggleWishlist_Add() throws Exception {
        // Given
        Long memberId = 1L;
        Long bookId = 100L;

        // 찜 추가 성공(true) 상황 설정
        given(wishlistService.toggleWishlist(memberId, bookId)).willReturn(true);

        // When & Then
        mockMvc.perform(post("/books/wishlists/{book-id}", bookId)
                        .header("X-Member-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isWished").value(true))
                .andExpect(jsonPath("$.message").value("찜 목록에 담았습니다."))
                .andDo(print());

        verify(wishlistService).toggleWishlist(memberId, bookId);
    }

    @Test
    @DisplayName("위시리스트 토글 - 찜 목록에서 삭제(False) 테스트")
    void toggleWishlist_Remove() throws Exception {
        // Given
        Long memberId = 1L;
        Long bookId = 100L;

        // 찜 해제(false) 상황 설정
        given(wishlistService.toggleWishlist(memberId, bookId)).willReturn(false);

        // When & Then
        mockMvc.perform(post("/books/wishlists/{book-id}", bookId)
                        .header("X-Member-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isWished").value(false))
                .andExpect(jsonPath("$.message").value("찜 목록에서 삭제했습니다."))
                .andDo(print());

        verify(wishlistService).toggleWishlist(memberId, bookId);
    }
}