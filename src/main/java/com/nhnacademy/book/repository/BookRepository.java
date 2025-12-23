package com.nhnacademy.book.repository;

import com.nhnacademy.book.client.order.dto.OrderBookResponse;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Modifying
    @Query("UPDATE Book b SET b.viewCount = b.viewCount + 1 WHERE b.bookId = :bookId")
    void updateViewCount(@Param("bookId") Long bookId);

    @Query("SELECT b, f.fileUrl " +
            "FROM Book b " +
            "LEFT JOIN FETCH b.publisher " +   // 출판사 Fetch Join (N+1 방지)
            "LEFT JOIN BookFile f ON b.bookId = f.joinedId AND f.fileType = :fileType") // 이미지 Join
    Page<Object[]> findAllBooksWithImage(Pageable pageable, @Param("fileType") FileType fileType);

    // [지금 많이 보는 도서] 조회수 높은 순으로 상위 10개
    List<Book> findTop10ByOrderByViewCountDesc();

    // 전체 신간 도서 Top 5 (출판일 내림차순, 동률 시 ID 내림차순)
    List<Book> findTop5ByOrderByBookPublicationDateDescBookIdDesc();

    // [카테고리별 신간] 특정 카테고리 + 최신순 + 5개 제한 (Pageable로 LIMIT 적용)
    @Query("SELECT b FROM Book b JOIN b.bookCategories bc " +
            "WHERE bc.category.categoryId = :categoryId " +
            "ORDER BY b.bookPublicationDate DESC")
    List<Book> findBooksByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // 주문 통신 전용 도서 배치 조회
    List<Book> findBooksByBookIdIn(List<Long> bookIds);

    // 주문 통신 전용 DTO 프로젝션
    @Query("""
        SELECT new com.nhnacademy.book.client.order.dto.OrderBookResponse(b.bookId, b.bookName, b.bookSalePrice, b.bookPackaging, f.fileUrl)
        FROM Book b
        LEFT JOIN BookFile f ON b.bookId = f.joinedId AND f.fileType = :fileType
        WHERE b.bookId IN :bookIds
    """)
    List<OrderBookResponse> findBooksInfoForOrderByIds(@Param("bookIds") List<Long> bookIds, @Param("fileType") FileType fileType);

    // 주문 통신 전용 재고 감소
    @Modifying
    @Query("UPDATE Book b SET b.bookStock = b.bookStock - :quantity WHERE b.bookId = :bookId AND b.bookStock >= :quantity")
    int decreaseStock(@Param("bookId") Long bookId, @Param("quantity") int quantity);

    // 주문 통신 전용 재고 증가
    @Modifying
    @Query("UPDATE Book b SET b.bookStock = b.bookStock + :quantity WHERE b.bookId = :bookId")
    void increaseStock(@Param("bookId") Long bookId, @Param("quantity") int quantity);

    boolean existsBookByIsbn(String isbn);

    // 카테고리 ID로 도서 목록 페이징 조회 (JPA 메서드 네이밍 규칙 활용)
    Page<Book> findByBookCategories_Category_CategoryId(Long categoryId, Pageable pageable);
}