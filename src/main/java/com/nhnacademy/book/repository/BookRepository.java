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
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Book b SET b.bookRegularPrice = 10000 WHERE b.bookRegularPrice = 0")
    int updateZeroRegularPricesToDefault();

    // 판매가가 0원인 책들을 정가와 똑같이 설정
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Book b SET b.bookSalePrice = b.bookRegularPrice WHERE b.bookSalePrice = 0")
    int updateZeroSalePricesToRegularPrice();

    @Query("SELECT b FROM Book b " +
            "WHERE (b.bookIndex IS NULL OR b.bookIndex = '')")
    List<Book> findAugmentationCandidates(String defaultImageUrl, Pageable pageable);

    @Modifying
    @Query("UPDATE Book b SET b.viewCount = b.viewCount + 1 WHERE b.bookId = :bookId")
    void updateViewCount(@Param("bookId") Long bookId);

    // 마이그레이션용: 이미지 URL이 'http'로 시작하는(아직 안 옮긴) 책 조회
    // List<Book> findByBookImageStartingWith(String prefix);

    // 이미지가 아직 남아있는(NULL이 아닌) 책들만 조회 -> 마이그레이션 대상
//    List<Book> findByBookImageIsNotNull();

    @Query("SELECT b, f.fileUrl " +
            "FROM Book b " +
            "LEFT JOIN FETCH b.publisher " +   // [1] 출판사: 매핑되어 있으니 'FETCH'로 성능 최적화 (N+1 방지)
            "LEFT JOIN BookFile f ON b.bookId = f.joinedId AND f.fileType = :fileType") // [2] 이미지: 매핑 없으니 직접 'ON'으로 조인
    Page<Object[]> findAllBooksWithImage(Pageable pageable, @Param("fileType") FileType fileType);

    List<OrderBookResponse> findAllBooksInBookIdsWithImage(List<Long> bookIds, @Param("fileType") FileType fileType);

    // 주문 통신 전용 DTO 프로젝션
    @Query("""
        SELECT new com.nhnacademy.book.client.order.dto.OrderBookResponse(b.bookId, b.bookName, b.bookSalePrice, b.bookPackaging, f.fileUrl)
        FROM Book b
        LEFT JOIN FETCH b.publisher
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
}
