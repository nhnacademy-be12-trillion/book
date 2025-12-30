package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/books/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

     // 위시리스트 조회 API
     // GET /api/books/wishlists
    @GetMapping
    public ResponseEntity<List<BookListResponse>> getWishlists(
            @RequestHeader(name = "X-Member-Id", required = false) Long memberId) {

        log.info("위시리스트 조회 요청 - Member ID: {}", memberId);

        List<BookListResponse> response = wishlistService.getWishlist(memberId);
        return ResponseEntity.ok(response);
    }

     // 위시리스트 토글 (담기/취소)
     // POST /api/books/wishlists/{bookId}
    @PostMapping("/{book-id}")
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @PathVariable("book-id") Long bookId,
            @RequestHeader(name = "X-Member-Id") Long memberId) {

        log.info("위시리스트 토글 요청 - Member ID: {}, Book ID: {}", memberId, bookId);

        boolean isWished = wishlistService.toggleWishlist(memberId, bookId);

        // 프론트엔드가 처리하기 좋게 JSON 형태로 응답
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isWished", isWished); // true: 찜 됨, false: 찜 해제됨
        response.put("message", isWished ? "찜 목록에 담았습니다." : "찜 목록에서 삭제했습니다.");

        return ResponseEntity.ok(response);
    }
}