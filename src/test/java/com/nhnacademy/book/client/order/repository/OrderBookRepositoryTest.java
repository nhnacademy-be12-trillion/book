package com.nhnacademy.book.client.order.repository;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.entity.Publisher;
import com.nhnacademy.book.exception.StockNotEnoughException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(OrderBookRepository.class)
@TestPropertySource(properties = "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl")
class OrderBookRepositoryTest {

    @Autowired
    private OrderBookRepository orderBookRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("재고 감소 - 정상 처리")
    void decreaseStock_Success() {
        // Given
        Publisher publisher = new Publisher("Test Publisher");
        entityManager.persist(publisher);

        Book book = Book.builder()
                .isbn("1234567890123")
                .bookName("Test Book")
                .bookDescription("Desc")
                .bookPublicationDate(LocalDate.now())
                .bookIndex("Index")
                .bookPackaging(true)
                .bookState(BookState.ON_SALE)
                .bookStock(10)
                .bookRegularPrice(10000)
                .bookSalePrice(9000)
                .bookReviewRate(0.0)
                .publisher(publisher)
                .build();
        
        book = entityManager.persist(book);
        entityManager.flush();
        entityManager.clear();

        Map<Long, Integer> quantityMap = Map.of(book.getBookId(), 2);

        // When
        orderBookRepository.decreaseStock(quantityMap);

        entityManager.clear();

        // Then
        Book updatedBook = entityManager.find(Book.class, book.getBookId());
        assertThat(updatedBook.getBookStock()).isEqualTo(8);
        assertThat(updatedBook.getBookState()).isEqualTo(BookState.ON_SALE);
    }

    @Test
    @DisplayName("재고 감소 - 품절 처리")
    void decreaseStock_SoldOut() {
        // Given
        Publisher publisher = new Publisher("Test Publisher");
        entityManager.persist(publisher);

        Book book = Book.builder()
                .isbn("1234567890123")
                .bookName("Test Book")
                .bookDescription("Desc")
                .bookPublicationDate(LocalDate.now())
                .bookIndex("Index")
                .bookPackaging(true)
                .bookState(BookState.ON_SALE)
                .bookStock(2)
                .bookRegularPrice(10000)
                .bookSalePrice(9000)
                .bookReviewRate(0.0)
                .publisher(publisher)
                .build();

        book = entityManager.persist(book);
        entityManager.flush();
        entityManager.clear();

        Map<Long, Integer> quantityMap = Map.of(book.getBookId(), 2);

        // When
        orderBookRepository.decreaseStock(quantityMap);

        entityManager.clear();

        // Then
        Book updatedBook = entityManager.find(Book.class, book.getBookId());
        assertThat(updatedBook.getBookStock()).isEqualTo(0);
        assertThat(updatedBook.getBookState()).isEqualTo(BookState.SOLD_OUT);
    }

    @Test
    @DisplayName("재고 감소 - 재고 부족 예외")
    void decreaseStock_Fail_StockNotEnough() {
        // Given
        Publisher publisher = new Publisher("Test Publisher");
        entityManager.persist(publisher);

        Book book = Book.builder()
                .isbn("1234567890123")
                .bookName("Test Book")
                .bookDescription("Desc")
                .bookPublicationDate(LocalDate.now())
                .bookIndex("Index")
                .bookPackaging(true)
                .bookState(BookState.ON_SALE)
                .bookStock(1)
                .bookRegularPrice(10000)
                .bookSalePrice(9000)
                .bookReviewRate(0.0)
                .publisher(publisher)
                .build();

        book = entityManager.persist(book);
        entityManager.flush();
        entityManager.clear();

        Map<Long, Integer> quantityMap = Map.of(book.getBookId(), 2);

        // When & Then
        Long bookId = book.getBookId();
        assertThatThrownBy(() -> orderBookRepository.decreaseStock(quantityMap))
                .isInstanceOf(StockNotEnoughException.class)
                .hasMessageContaining("재고가 부족한 도서가 포함되어 있습니다: " + bookId);
    }

    @Test
    @DisplayName("재고 증가 - 정상 처리 (상태 복구)")
    void bulkIncreaseStock_Success_StateRestore() {
        // Given
        Publisher publisher = new Publisher("Test Publisher");
        entityManager.persist(publisher);

        Book book = Book.builder()
                .isbn("1234567890123")
                .bookName("Test Book")
                .bookDescription("Desc")
                .bookPublicationDate(LocalDate.now())
                .bookIndex("Index")
                .bookPackaging(true)
                .bookState(BookState.SOLD_OUT)
                .bookStock(0)
                .bookRegularPrice(10000)
                .bookSalePrice(9000)
                .bookReviewRate(0.0)
                .publisher(publisher)
                .build();

        book = entityManager.persist(book);
        entityManager.flush();
        entityManager.clear();

        Map<Long, Integer> quantityMap = Map.of(book.getBookId(), 5);

        // When
        orderBookRepository.bulkIncreaseStock(quantityMap);

        entityManager.clear();

        // Then
        Book updatedBook = entityManager.find(Book.class, book.getBookId());
        assertThat(updatedBook.getBookStock()).isEqualTo(5);
        assertThat(updatedBook.getBookState()).isEqualTo(BookState.ON_SALE);
    }
}