package com.nhnacademy.book.controller.docs;

import com.nhnacademy.book.dto.book.BookListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@Tag(name = "Wishlist", description = "위시리스트(찜) API")
public interface WishlistControllerDocs {

    @Operation(summary = "위시리스트 조회", description = "내 위시리스트 목록을 조회합니다.")
    ResponseEntity<List<BookListResponse>> getWishlists(
            @Parameter(description = "회원 ID") Long memberId
    );

    @Operation(summary = "위시리스트 토글", description = "도서를 위시리스트에 담거나 취소합니다.")
    ResponseEntity<Map<String, Object>> toggleWishlist(
            @Parameter(description = "도서 ID", required = true) Long bookId,
            @Parameter(description = "회원 ID", required = true) Long memberId
    );
}
