package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.exception.BookNotFoundException;
import com.nhnacademy.book.exception.WishlistNotFoundException;
import com.nhnacademy.book.repository.BookFileRepository;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.WishlistRepository;
import com.nhnacademy.book.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;
    private final BookFileRepository bookFileRepository;

    @Override
    public boolean toggleWishlist(Long memberId, Long bookId) {

        // 도서 조회
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("도서 정보를 찾을 수 없습니다. ID: " + bookId));

        // 중복 확인 및 토글 로직
        if (wishlistRepository.existsByMemberIdAndBook(memberId, book)) {
            Wishlist wishlist = wishlistRepository.findByMemberIdAndBook(memberId, book)
                    .orElseThrow(() -> new WishlistNotFoundException("데이터 불일치: 찜 내역이 존재해야 합니다."));
            wishlistRepository.delete(wishlist);
            return false; // 찜 취소
        } else {
            Wishlist newWishlist = Wishlist.create(memberId, book);
            wishlistRepository.save(newWishlist);
            return true; // 찜 성공
        }
    }

    @Override
    // 조회 메서드지만 내부에서 '삭제(delete)'가 발생 가능 readOnly = true를 제거
    public List<BookListResponse> getWishlist(Long memberId) {
        // 회원의 전체 위시리스트 조회
        List<Wishlist> wishlists = wishlistRepository.findByMemberId(memberId);

        if (wishlists.isEmpty()) {
            return List.of();
        }

        // SALE_END(판매 종료) 상태인 도서는 DB에서 삭제하고, 조회 목록에서도 제외
        List<Wishlist> activeWishlists = wishlists.stream()
                .filter(wishlist -> {
                    // 책 상태 확인
                    if (wishlist.getBook().getBookState() == BookState.SALE_END) {
                        // 판매 종료된 책은 찜 목록에서 영구 삭제
                        wishlistRepository.delete(wishlist);
                        return false; // 리스트에 포함하지 않음
                    }
                    return true; // 리스트에 포함
                })
                .collect(Collectors.toList());

        // 삭제 후 남은 유효한 위시리스트가 없으면 빈 리스트 반환
        if (activeWishlists.isEmpty()) {
            return List.of();
        }

        // 유효한 도서들의 ID 목록 추출
        List<Long> bookIds = activeWishlists.stream()
                .map(wishlist -> wishlist.getBook().getBookId())
                .collect(Collectors.toList());

        // 책 이미지 일괄 조회
        List<BookFile> bookFiles = bookFileRepository.findAllByJoinedIdInAndFileType(bookIds, FileType.BOOK);

        // 이미지를 Map으로 변환 (ID -> ImageUrl)
        Map<Long, String> bookImageMap = bookFiles.stream()
                .collect(Collectors.toMap(
                        BookFile::getJoinedId,
                        BookFile::getFileUrl,
                        (existing, replacement) -> existing
                ));

        // 응답 DTO 변환 및 반환
        return activeWishlists.stream()
                .map(wishlist -> {
                    Book book = wishlist.getBook();
                    String imageUrl = bookImageMap.get(book.getBookId());
                    return BookListResponse.from(book, imageUrl);
                })
                .collect(Collectors.toList());
    }
}