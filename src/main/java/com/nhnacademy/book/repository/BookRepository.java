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

    // 특정 도서의 조회수를 1 증가
    @Modifying
    @Query("UPDATE Book b SET b.viewCount = b.viewCount + 1 WHERE b.bookId = :bookId")
    void updateViewCount(@Param("bookId") Long bookId);

    // 도서 목록과 대표 이미지 URL을 함께 페이징 조회
    @Query("SELECT b, f.fileUrl " +
            "FROM Book b " +
            "LEFT JOIN FETCH b.publisher " +
            "LEFT JOIN BookFile f ON b.bookId = f.joinedId AND f.fileType = :fileType " +
            "WHERE b.bookState <> com.nhnacademy.book.entity.BookState.SALE_END")
    Page<Object[]> findAllBooksWithImage(Pageable pageable, @Param("fileType") FileType fileType);

    // 조회수 기준 상위 10개 도서 조회
    List<Book> findTop10ByOrderByViewCountDesc();


    // 특정 카테고리에 속한 신간 도서 목록 조회
    @Query("SELECT b FROM Book b JOIN b.bookCategories bc " +
            "WHERE bc.category.categoryId = :categoryId " +
            "AND b.bookState <> com.nhnacademy.book.entity.BookState.SALE_END " + // id != null 제거
            "ORDER BY b.bookPublicationDate DESC ")
    List<Book> findBooksByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
    // 여러 도서 ID에 해당하는 도서 목록 조회
    List<Book> findBooksByBookIdIn(List<Long> bookIds);

    // 주문 처리를 위해 필요한 도서 정보 조회
    @Query("""
        SELECT new com.nhnacademy.book.client.order.dto.OrderBookResponse(b.bookId, b.bookName, b.bookSalePrice, b.bookPackaging, f.fileUrl)
        FROM Book b
        LEFT JOIN BookFile f ON b.bookId = f.joinedId AND f.fileType = :fileType
        WHERE b.bookId IN :bookIds
    """)
    List<OrderBookResponse> findBooksInfoForOrderByIds(@Param("bookIds") List<Long> bookIds, @Param("fileType") FileType fileType);

    // ISBN 기준 도서 존재 여부 확인
    boolean existsBookByIsbn(String isbn);

    // 카테고리 ID 기준 도서 목록 페이징 조회
    @Query("SELECT b " +
            "FROM Book b " +
            "JOIN b.bookCategories bc " +
            "WHERE bc.category.categoryId = :categoryId " +
            "AND b.bookState <> com.nhnacademy.book.entity.BookState.SALE_END")
    Page<Book> findByBookCategories_Category_CategoryId(Long categoryId, Pageable pageable);
}