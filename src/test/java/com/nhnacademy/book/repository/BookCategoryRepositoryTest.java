package com.nhnacademy.book.repository;

import com.nhnacademy.book.RepositoryTest;
import com.nhnacademy.book.dto.category.BookWithCategory;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookCategory;
import com.nhnacademy.book.entity.Category;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryTest
class BookCategoryRepositoryTest {
    @Autowired
    private BookCategoryRepository bookCategoryRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("없으면 빈 리스트가 나온다.")
    void getBookCategoryByIdTest() {
        List<BookWithCategory> bookCategoryIds = bookCategoryRepository.findBookCategoryIds(List.of(12L));
        Assertions.assertThat(bookCategoryIds).isEmpty();
    }

    @Test
    @DisplayName("있으면 값이 나온다.")
    void getBookCategoryByIdTest1() {
        Book book = Book.builder().build();
        Book book1 = Book.builder().build();

        Category category = new Category(0L,"qwe");
        BookCategory bookCategory= new BookCategory(category,book);
        Category category1 = new Category(1L,"asd");
        BookCategory bookCategory1= new BookCategory(category1,book);
        BookCategory bookCategory2= new BookCategory(category1,book1);


        categoryRepository.save(category);
        categoryRepository.save(category1);

        bookRepository.save(book);
        bookRepository.save(book1);

        bookCategoryRepository.save(bookCategory);
        bookCategoryRepository.save(bookCategory1);
        bookCategoryRepository.save(bookCategory2);

        List<BookWithCategory> bookCategoryIds = bookCategoryRepository.findBookCategoryIds(List.of(1L,2L));
        Assertions.assertThat(bookCategoryIds).hasSize(3);
    }

}