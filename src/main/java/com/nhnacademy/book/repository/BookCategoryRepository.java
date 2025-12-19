package com.nhnacademy.book.repository;

import com.nhnacademy.book.dto.category.BookCategoryResponse;
import com.nhnacademy.book.entity.BookCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {

    @Query("select b.book.bookId,b.category.categoryId from BookCategory b where b.book.bookId in :bookIds")
    List<BookCategoryResponse> findBookCategoryIds(@Param("bookIds") List<Long> bookIds);
}
