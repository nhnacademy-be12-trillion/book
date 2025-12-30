package com.nhnacademy.book.repository;

import com.nhnacademy.book.dto.category.BookWithCategory;
import com.nhnacademy.book.entity.BookCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {

    // 여러 도서 ID에 대해 도서 기본 정보와 카테고리 ID를 함께 조회
    @Query("select b.bookId,b.isbn,b.bookStock,b.bookSalePrice,c.category.categoryId from BookCategory c join c.book b where b.bookId in :bookIds")
    List<BookWithCategory> findBookCategoryIds(@Param("bookIds") List<Long> bookIds);
}
