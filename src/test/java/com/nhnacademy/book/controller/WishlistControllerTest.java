package com.nhnacademy.book.controller;

import com.nhnacademy.book.service.WishlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WishlistController.class, properties = "spring.cloud.config.enabled=false")
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistService wishlistService;

    @Test
    @DisplayName("찜 추가 (Toggle On) - 서비스가 true 반환 시 '담았습니다' 메시지 응답")
    void toggleWishlist_Add() throws Exception {
        // given
        Long bookId = 10L;
        Long memberId = 1L; // Controller에 하드코딩된 테스트 ID

        // 서비스가 true(찜 추가됨)를 반환한다고 가정
        given(wishlistService.toggleWishlist(memberId, bookId)).willReturn(true);

        // when & then
        mockMvc.perform(post("/api/wishlists/{bookId}", bookId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isWished").value(true))
                .andExpect(jsonPath("$.message").value("찜 목록에 담았습니다."))
                .andDo(print());

        verify(wishlistService).toggleWishlist(memberId, bookId);
    }

    @Test
    @DisplayName("찜 취소 (Toggle Off) - 서비스가 false 반환 시 '삭제했습니다' 메시지 응답")
    void toggleWishlist_Remove() throws Exception {
        // given
        Long bookId = 10L;
        Long memberId = 1L;

        // 서비스가 false(찜 취소됨)를 반환한다고 가정
        given(wishlistService.toggleWishlist(memberId, bookId)).willReturn(false);

        // when & then
        mockMvc.perform(post("/api/wishlists/{bookId}", bookId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isWished").value(false))
                .andExpect(jsonPath("$.message").value("찜 목록에서 삭제했습니다."))
                .andDo(print());

        verify(wishlistService).toggleWishlist(memberId, bookId);
    }
}