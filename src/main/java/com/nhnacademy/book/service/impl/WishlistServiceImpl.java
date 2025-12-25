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
    @Transactional(readOnly = true)
    public List<BookListResponse> getWishlist(Long memberId) {
        // 회원의 위시리스트 조회
        List<Wishlist> wishlists = wishlistRepository.findByMemberId(memberId);

        if (wishlists.isEmpty()) {
            return List.of();
        }

        // 조회된 위시리스트에서 책 ID 목록 추출
        List<Long> bookIds = wishlists.stream()
                .map(wishlist -> wishlist.getBook().getBookId())
                .collect(Collectors.toList());

        // 책 ID 목록에 해당하는 이미지들을 한 번의 쿼리로 조회 (IN 절 사용)
        List<BookFile> bookFiles = bookFileRepository.findAllByJoinedIdInAndFileType(bookIds, FileType.BOOK);

        // 조회된 이미지들을 Map으로 변환
        Map<Long, String> bookImageMap = bookFiles.stream()
                .collect(Collectors.toMap(
                        BookFile::getJoinedId, // Key
                        BookFile::getFileUrl,  // Value
                        (existing, replacement) -> existing // 혹시 중복된 이미지가 있다면 기존 것 유지
                ));

        // BookListResponse 변환 및 반환
        return wishlists.stream()
                .map(wishlist -> {
                    Book book = wishlist.getBook();
                    // 맵에서 해당 책의 이미지 URL을 찾고, 없으면 null 혹은 기본 이미지
                    String imageUrl = bookImageMap.get(book.getBookId());

                    return BookListResponse.from(book, imageUrl);
                })
                .collect(Collectors.toList());
    }
}