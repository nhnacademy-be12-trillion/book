package com.nhnacademy.book.repository;

import com.nhnacademy.book.dto.category.BookWithCategory;
import com.nhnacademy.book.entity.BookCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {

    @Query("select b.bookId,b.isbn,b.bookStock,b.bookSalePrice,c.category.categoryId from BookCategory c join Book  b on c.book.bookId=b.bookId where c.book.bookId in :bookIds")
    List<BookWithCategory> findBookCategoryIds(@Param("bookIds") List<Long> bookIds);
}
