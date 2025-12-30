package com.nhnacademy.book.repository;

import com.nhnacademy.book.client.order.dto.OrderBookResponse;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.entity.FileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    // 테스트용 도서 생성 헬퍼 메서드
    private Book createBook(String isbn, String title, int price, int viewCount) {
        Book book = Book.builder()
                .isbn(isbn)
                .bookName(title)
                .bookDescription("설명")
                .bookPublicationDate(LocalDate.now())
                .bookIndex("목차")
                .bookPackaging(true)
                .bookState(BookState.ON_SALE)
                .bookStock(100)
                .bookRegularPrice(price + 2000)
                .bookSalePrice(price)
                .bookReviewRate(0.0)
                .publisher(null) // Publisher 테스트 생략 시 null
                .build();

        return book;
    }

    @Test
    @DisplayName("조회수 증가 업데이트 쿼리 검증")
    void updateViewCount() {
        // given
        Book book = createBook("978-1111", "조회수 테스트 책", 10000, 0);
        entityManager.persist(book);
        entityManager.flush(); // DB 반영

        // when
        bookRepository.updateViewCount(book.getBookId());

        // @Modifying 쿼리 후에는 영속성 컨텍스트를 비워야 갱신된 값을 다시 가져옴
        entityManager.clear();

        // then
        Book updatedBook = bookRepository.findById(book.getBookId()).orElseThrow();
        assertThat(updatedBook.getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("도서 목록과 이미지 URL 페이징 조회 (Left Join)")
    void findAllBooksWithImage() {
        // given
        Book book = createBook("978-2222", "이미지 테스트 책", 15000, 0);
        entityManager.persist(book);

        // BookFile 생성 (FK인 joinedId에 bookId 설정)
        BookFile bookFile = BookFile.builder()
                .fileUrl("http://test-image.com/1.jpg")
                .fileType(FileType.BOOK)
                .joinedId(book.getBookId())
                .build();
        entityManager.persist(bookFile);

        entityManager.flush();
        entityManager.clear();

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> result = bookRepository.findAllBooksWithImage(pageable, FileType.BOOK);

        // then
        assertThat(result.getContent()).hasSize(1);
        Object[] row = result.getContent().get(0);
        Book resultBook = (Book) row[0];
        String resultUrl = (String) row[1];

        assertThat(resultBook.getIsbn()).isEqualTo("978-2222");
        assertThat(resultUrl).isEqualTo("http://test-image.com/1.jpg");
    }

    @Test
    @DisplayName("주문용 도서 정보 DTO Projection 조회")
    void findBooksInfoForOrderByIds() {
        // given
        Book book1 = createBook("978-3333", "주문 책 1", 20000, 0);
        Book book2 = createBook("978-4444", "주문 책 2", 30000, 0);
        entityManager.persist(book1);
        entityManager.persist(book2);

        // book1만 이미지를 가짐
        BookFile file1 = BookFile.builder()
                .fileUrl("http://img.com/book1.jpg")
                .fileType(FileType.BOOK)
                .joinedId(book1.getBookId())
                .build();
        entityManager.persist(file1);

        entityManager.flush();
        entityManager.clear();

        // when
        List<Long> targetIds = List.of(book1.getBookId(), book2.getBookId());
        List<OrderBookResponse> responses = bookRepository.findBooksInfoForOrderByIds(targetIds, FileType.BOOK);

        // then
        assertThat(responses).hasSize(2);

        // Book1 검증 (이미지 있음)
        OrderBookResponse res1 = responses.stream()
                .filter(r -> r.bookId().equals(book1.getBookId()))
                .findFirst().orElseThrow();
        assertThat(res1.bookName()).isEqualTo("주문 책 1");
        assertThat(res1.imageUrl()).isEqualTo("http://img.com/book1.jpg");
        assertThat(res1.price()).isEqualTo(20000);

        // Book2 검증 (이미지 없음 -> Left Join이므로 null이어야 함)
        OrderBookResponse res2 = responses.stream()
                .filter(r -> r.bookId().equals(book2.getBookId()))
                .findFirst().orElseThrow();
        assertThat(res2.bookName()).isEqualTo("주문 책 2");
        assertThat(res2.imageUrl()).isNull();
    }

    @Test
    @DisplayName("ISBN으로 도서 존재 여부 확인")
    void existsBookByIsbn() {
        // given
        String isbn = "978-9999";
        Book book = createBook(isbn, "존재 확인 책", 10000, 0);
        entityManager.persist(book);

        // when & then
        assertThat(bookRepository.existsBookByIsbn(isbn)).isTrue();
        assertThat(bookRepository.existsBookByIsbn("000-0000")).isFalse();
    }

    @Test
    @DisplayName("조회수 기준 상위 10개 조회")
    void findTop10ByOrderByViewCountDesc() {
        // given

        Book popularBook = createBook("978-POP", "인기 책", 10000, 0);
        entityManager.persist(popularBook);
        Book normalBook = createBook("978-NORM", "보통 책", 10000, 0);
        entityManager.persist(normalBook);

        entityManager.flush();

        // popularBook 조회수 2 증가
        bookRepository.updateViewCount(popularBook.getBookId());
        bookRepository.updateViewCount(popularBook.getBookId());

        entityManager.clear(); // 영속성 컨텍스트 초기화

        // when
        List<Book> results = bookRepository.findTop10ByOrderByViewCountDesc();

        // then
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results.get(0).getIsbn()).isEqualTo("978-POP"); // 1등 확인
        assertThat(results.get(0).getViewCount()).isEqualTo(2);
    }
}