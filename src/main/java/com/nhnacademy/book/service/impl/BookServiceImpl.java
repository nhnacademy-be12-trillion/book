package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.BookCreateRequest;
import com.nhnacademy.book.dto.BookDetailResponse;
import com.nhnacademy.book.dto.BookListResponse;
import com.nhnacademy.book.dto.BookUpdateRequest;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 최적화를 위해 기본 설정
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    // 1. 도서 목록 조회 구현 (BookListResponse 사용)
    @Override
    public Page<BookListResponse> getBooks(Pageable pageable) {
        // JPA로 모든 Entity를 가져온 후, List DTO로 변환하여 반환
        return bookRepository.findAll(pageable)
                .map(BookListResponse::from);
    }

    // 2. 도서 상세 조회 구현 (BookDetailResponse 사용)
    @Override
    public BookDetailResponse getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 도서가 존재하지 않습니다. ID: " + bookId));

        // Entity를 Detail DTO로 변환하여 반환
        return BookDetailResponse.from(book);
    }

    // 3. 도서 등록 구현 (BookCreateRequest 사용)
    @Override
    @Transactional // 쓰기 작업이므로 트랜잭션 필요
    public Long createBook(BookCreateRequest request) {
        // 1. DTO -> Entity 변환
        Book book = new Book();

        // request 필드 매핑
        book.setIsbn(request.getIsbn());
        book.setBookName(request.getBookName());
        book.setBookDescription(request.getBookDescription());
        book.setBookPublisher(request.getBookPublisher());
        book.setBookPublicationDate(request.getBookPublicationDate());
        book.setBookIndex(request.getBookIndex());
        book.setBookPackaging(request.isBookPackaging());
        book.setBookState(request.getBookState());
        book.setBookStock(request.getBookStock());
        book.setBookRegularPrice(request.getBookRegularPrice());
        book.setBookSalePrice(request.getBookSalePrice());
        book.setBookImage(request.getBookImage());

        // 누락된 필드 기본값 설정 (리뷰 점수는 등록 시 0.0)
        book.setBookReviewRate(0.0);

        // 2. DB 저장 및 ID 반환
        Book savedBook = bookRepository.save(book);
        return savedBook.getBookId();
    }

    // 4. 도서 정보 수정 (Update Request DTO 사용)
    @Override
    @Transactional // 쓰기 작업이므로 @Transactional을 붙여야 합니다.
    public void updateBook(Long bookId, BookUpdateRequest request) {

        // 1. 수정할 Entity를 DB에서 조회 (없으면 예외 발생)
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 도서가 없습니다. ID: " + bookId));

        // 2. 핵심 로직: 할인율을 적용하여 판매가를 계산합니다.
        int regularPrice = book.getBookRegularPrice();
        double discountRate = request.getDiscountRate();

        // 판매가 계산: 정가 * (1 - 할인율/100)
        int newSalePrice = (int) Math.round(regularPrice * (1 - discountRate / 100.0));

        // 3. 요청받은 값과 계산된 판매가로 Entity 필드를 수정합니다.
        // (JPA의 변경 감지(Dirty Checking) 기능으로 자동 저장됩니다.)
        book.setBookName(request.getBookName());
        book.setBookDescription(request.getBookDescription());
        book.setBookIndex(request.getBookIndex());
        book.setBookPackaging(request.isBookPackaging());
        book.setBookState(request.getBookState());
        book.setBookStock(request.getBookStock());
        book.setBookImage(request.getBookImage());

        // 계산된 판매가 반영
        book.setBookSalePrice(newSalePrice);

        // @Transactional 메서드가 끝날 때, 변경된 내용이 자동으로 DB에 반영됩니다.
    }

    // 5. 도서 삭제 (물리적 삭제 대신 bookState를 SALE_END로 변경)
    @Override
    @Transactional // 상태를 변경하는 쓰기 작업이므로 @Transactional 필요
    public void deleteBook(Long bookId) {

        // 1. 삭제할 Entity를 DB에서 조회 (없으면 예외 발생)
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 도서가 없습니다. ID: " + bookId));

        // 2. 핵심 로직: BookState를 '판매 종료' 상태로 변경합니다.
        book.setBookState(BookState.SALE_END);

        // @Transactional이 설정되어 있으므로, 이 시점에 변경 감지(Dirty Checking)를 통해
        // 별도로 save()를 호출하지 않아도 DB에 상태가 자동 반영됩니다.
    }
}