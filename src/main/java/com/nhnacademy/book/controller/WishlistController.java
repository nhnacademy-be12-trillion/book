package com.nhnacademy.book.controller;

import com.nhnacademy.book.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{bookId}")
    public ResponseEntity<Map<String, Object>> toggleWishlist(@PathVariable Long bookId) {

        // 현재는 테스트 단계이므로 TestMemberInitializer가 만든 '1번 회원'이라고 가정
        // 나중에 Spring Security나 세션이 적용되면 아래 코드를
        // Long memberId = (Long) session.getAttribute("memberId"); 등으로 바꾸기
        Long currentMemberId = 1L;

        boolean isWished = wishlistService.toggleWishlist(currentMemberId, bookId);

        // 프론트엔드가 처리하기 좋게 JSON 형태로 응답
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isWished", isWished); // true면 하트 채움, false면 하트 비움
        response.put("message", isWished ? "찜 목록에 담았습니다." : "찜 목록에서 삭제했습니다.");

        return ResponseEntity.ok(response);
    }
}