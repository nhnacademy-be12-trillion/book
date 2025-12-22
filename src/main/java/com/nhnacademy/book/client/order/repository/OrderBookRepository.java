package com.nhnacademy.book.client.order.repository;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.exception.StockNotEnoughException;
import com.nhnacademy.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderBookRepository {
    private final JdbcTemplate jdbcTemplate;
    private final BookRepository bookRepository;

    public void decreaseStock(Map<Long, Integer> quantityMap) {
        List<Long> bookIds = new ArrayList<>(quantityMap.keySet());
        
        // 1. 필요한 모든 책의 정보를 배치 조회
        Map<Long, Book> bookMap = bookRepository.findBooksByBookIdIn(bookIds).stream()
                .collect(Collectors.toMap(Book::getBookId, Function.identity()));

        // 2. 재고가 충분한지 확인
        for (Long bookId : bookIds) {
            Book book = bookMap.get(bookId);
            if (book == null || book.getBookStock() < quantityMap.get(bookId)) {
                throw new StockNotEnoughException("재고가 부족한 도서가 포함되어 있습니다: " + bookId);
            }
        }

        // 3. JdbcTemplate을 사용하여 Batch Update 실행
        String sql = "UPDATE Book SET bookStock = ?, bookState = ? WHERE bookId = ?";

        jdbcTemplate.batchUpdate(sql, bookIds, 100, (PreparedStatement ps, Long bookId) -> {
            Book book = bookMap.get(bookId);
            int quantity = quantityMap.get(bookId);
            int newStock = book.getBookStock() - quantity;
            
            String newState = (newStock == 0) ? BookState.SOLD_OUT.name() : book.getBookState().name();

            ps.setInt(1, newStock);
            ps.setString(2, newState);
            ps.setLong(3, bookId);
        });
    }

    public void bulkIncreaseStock(Map<Long, Integer> quantityMap) {
        List<Long> bookIds = new ArrayList<>(quantityMap.keySet());

        // 1. 필요한 모든 책의 정보를 한 번의 쿼리로 가져옴
        Map<Long, Book> bookMap = bookRepository.findBooksByBookIdIn(bookIds).stream()
                .collect(Collectors.toMap(Book::getBookId, Function.identity()));

        // 2. JdbcTemplate을 사용하여 Batch Update를 실행
        String sql = "UPDATE Book SET bookStock = ?, bookState = ? WHERE bookId = ?";

        jdbcTemplate.batchUpdate(sql, bookIds, 100, (PreparedStatement ps, Long bookId) -> {
            Book book = bookMap.get(bookId);
            int quantity = quantityMap.get(bookId);
            int newStock = book.getBookStock() + quantity;

            // 현재 상태가 SOLD_OUT 이었다면 ON_SALE으로 변경
            String newState = (book.getBookState() == BookState.SOLD_OUT) ?
                    BookState.ON_SALE.name() : book.getBookState().name();

            ps.setInt(1, newStock);
            ps.setString(2, newState);
            ps.setLong(3, bookId);
        });
    }
}
