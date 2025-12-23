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
            "LEFT JOIN FETCH b.publisher " +
            "LEFT JOIN BookFile f ON b.bookId = f.joinedId AND f.fileType = :fileType")
    Page<Object[]> findAllBooksWithImage(Pageable pageable, @Param("fileType") FileType fileType);

    List<Book> findTop10ByOrderByViewCountDesc();

    List<Book> findTop5ByOrderByBookPublicationDateDescBookIdDesc();

    // [★필수] 카테고리별 신간 조회 쿼리
    @Query("SELECT b FROM Book b JOIN b.bookCategories bc " +
            "WHERE bc.category.categoryId = :categoryId " +
            "ORDER BY b.bookPublicationDate DESC")
    List<Book> findBooksByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // 기존 메서드들 유지
    List<Book> findBooksByBookIdIn(List<Long> bookIds);

    @Query("""
        SELECT new com.nhnacademy.book.client.order.dto.OrderBookResponse(b.bookId, b.bookName, b.bookSalePrice, b.bookPackaging, f.fileUrl)
        FROM Book b
        LEFT JOIN BookFile f ON b.bookId = f.joinedId AND f.fileType = :fileType
        WHERE b.bookId IN :bookIds
    """)
    List<OrderBookResponse> findBooksInfoForOrderByIds(@Param("bookIds") List<Long> bookIds, @Param("fileType") FileType fileType);

    @Modifying
    @Query("UPDATE Book b SET b.bookStock = b.bookStock - :quantity WHERE b.bookId = :bookId AND b.bookStock >= :quantity")
    int decreaseStock(@Param("bookId") Long bookId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Book b SET b.bookStock = b.bookStock + :quantity WHERE b.bookId = :bookId")
    void increaseStock(@Param("bookId") Long bookId, @Param("quantity") int quantity);

    boolean existsBookByIsbn(String isbn);

    Page<Book> findByBookCategories_Category_CategoryId(Long categoryId, Pageable pageable);
}