package com.nhnacademy.book.repository;

import com.nhnacademy.book.dto.category.BookWithCategory;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookCategory;
import com.nhnacademy.book.entity.Category;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class BookCategoryRepositoryTest {
    @Autowired
    private BookCategoryRepository bookCategoryRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TestEntityManager testEntityManager;
    @Test
    @DisplayName("없으면 빈 리스트가 나온다.")
    void getBookCategoryByIdTest() {
        List<BookWithCategory> bookCategoryIds = bookCategoryRepository.findBookCategoryIds(List.of(12L));
        Assertions.assertThat(bookCategoryIds).isEmpty();
    }

    @Test
    @DisplayName("있으면 값이 나온다.")
    void getBookCategoryByIdTest1() {

        Category category = new Category(1L,"qwe");
        Category category1 = new Category(2L,"asd");
        Book book = bookRepository.save(Book.builder().build());
        Book book1=bookRepository.save(Book.builder().build());


        BookCategory bookCategory= new BookCategory(category,book);
        BookCategory bookCategory1= new BookCategory(category1,book);
        BookCategory bookCategory2= new BookCategory(category1,book1);

        categoryRepository.save(category);
        categoryRepository.save(category1);


        bookCategoryRepository.save(bookCategory);
        bookCategoryRepository.save(bookCategory1);
        bookCategoryRepository.save(bookCategory2);

        List<BookWithCategory> bookCategoryIds = bookCategoryRepository.findBookCategoryIds(List.of(book.getBookId(),book1.getBookId()));
        Assertions.assertThat(bookCategoryIds).hasSize(3);
    }

}