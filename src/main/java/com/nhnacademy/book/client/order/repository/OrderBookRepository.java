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
        // 만약 동시에 도서 ID가 (2, 1)인 주문과 (1, 2)인 주문 처리 시 데드락 발생 위험
        // 데드락 방지를 위해 도서 ID를 순서대로 처리
        List<Long> bookIds = quantityMap.keySet().stream()
                .sorted()
                .toList();
        
        // 상태 변경 로직을 위해 현재 도서 정보를 가져옴
        Map<Long, Book> bookMap = bookRepository.findBooksByBookIdIn(bookIds).stream()
                .collect(Collectors.toMap(Book::getBookId, Function.identity()));

        String sql = "UPDATE Book SET bookStock = bookStock - ?, bookState = ? WHERE bookId = ? AND bookStock >= ?";

        int[][] updateCounts = jdbcTemplate.batchUpdate(sql, bookIds, 100, (PreparedStatement ps, Long bookId) -> {
            int quantity = quantityMap.get(bookId);
            Book book = bookMap.get(bookId);

            // 오직 상태 결정을 위한 계산
            int newStockForStateCheck = book.getBookStock() - quantity;
            String newState = (newStockForStateCheck == 0) ? BookState.SOLD_OUT.name() : book.getBookState().name();

            ps.setInt(1, quantity);      // 1: 차감할 수량
            ps.setString(2, newState);   // 2: 새로운 상태
            ps.setLong(3, bookId);       // 3: 도서 ID
            ps.setInt(4, quantity);      // 4: WHERE 절에서 확인할 수량
        });

        // 배치 업데이트 결과 확인
        int bookIndex = 0;
        for (int[] batchResult : updateCounts) {
            for (int count : batchResult) {
                if (count == 0) {
                    Long failedBookId = bookIds.get(bookIndex);
                    throw new StockNotEnoughException("재고가 부족한 도서가 포함되어 있습니다: " + failedBookId);
                }
                bookIndex++;
            }
        }
    }

    public void bulkIncreaseStock(Map<Long, Integer> quantityMap) {
        List<Long> bookIds = quantityMap.keySet().stream()
                .sorted()
                .toList();

        // 상태 변경 로직을 위해 현재 도서 정보를 가져옴
        Map<Long, Book> bookMap = bookRepository.findBooksByBookIdIn(bookIds).stream()
                .collect(Collectors.toMap(Book::getBookId, Function.identity()));

        String sql = "UPDATE Book SET bookStock = bookStock + ?, bookState = ? WHERE bookId = ?";

        jdbcTemplate.batchUpdate(sql, bookIds, 100, (PreparedStatement ps, Long bookId) -> {
            Book book = bookMap.get(bookId);
            int quantity = quantityMap.get(bookId);

            // 현재 상태가 SOLD_OUT 이었다면 ON_SALE으로 변경
            String newState = (book.getBookState() == BookState.SOLD_OUT) ?
                    BookState.ON_SALE.name() : book.getBookState().name();

            ps.setInt(1, quantity);     // 1: 증가시킬 수량
            ps.setString(2, newState);  // 2: 새로운 상태
            ps.setLong(3, bookId);      // 3: 도서 ID
        });
    }
}
