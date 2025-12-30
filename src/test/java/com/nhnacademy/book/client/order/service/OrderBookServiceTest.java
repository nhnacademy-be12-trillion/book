package com.nhnacademy.book.client.order.service;

import com.nhnacademy.book.client.order.dto.OrderBookResponse;
import com.nhnacademy.book.client.order.repository.OrderBookRepository;
import com.nhnacademy.book.client.order.saga.domain.OrderBookSagaLog;
import com.nhnacademy.book.client.order.saga.domain.OrderBookSagaLogId;
import com.nhnacademy.book.client.order.saga.domain.OrderSagaType;
import com.nhnacademy.book.client.order.saga.repository.OrderBookSagaLogRepository;
import com.nhnacademy.book.entity.FileType;
import com.nhnacademy.book.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderBookServiceTest {

    @InjectMocks
    private OrderBookService orderBookService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private OrderBookSagaLogRepository orderBookSagaLogRepository;

    @Mock
    private OrderBookRepository orderBookRepository;

    @Test
    @DisplayName("도서 정보 조회")
    void getAllBookByBookIds() {
        List<Long> bookIds = List.of(1L, 2L);
        OrderBookResponse response = new OrderBookResponse(1L, "Title", 1000, true, "url");
        when(bookRepository.findBooksInfoForOrderByIds(bookIds, FileType.BOOK))
                .thenReturn(List.of(response));

        List<OrderBookResponse> result = orderBookService.getAllBookByBookIds(bookIds);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bookId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("재고 감소 - 이미 처리된 사가")
    void decreaseStock_AlreadyProcessed() {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 1);

        when(orderBookSagaLogRepository.existsById(any(OrderBookSagaLogId.class))).thenReturn(true);

        orderBookService.decreaseStock(sagaId, quantityMap);

        verify(orderBookRepository, never()).decreaseStock(any());
        verify(orderBookSagaLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 감소 - 정상 처리")
    void decreaseStock_Success() {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 1);

        when(orderBookSagaLogRepository.existsById(any(OrderBookSagaLogId.class))).thenReturn(false);

        orderBookService.decreaseStock(sagaId, quantityMap);

        verify(orderBookRepository).decreaseStock(quantityMap);
        verify(orderBookSagaLogRepository).save(any(OrderBookSagaLog.class));
    }

    @Test
    @DisplayName("재고 증가 - 이미 처리된 사가")
    void increaseStock_AlreadyProcessed() {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 1);

        when(orderBookSagaLogRepository.existsById(any(OrderBookSagaLogId.class))).thenReturn(true);

        orderBookService.increaseStock(sagaId, quantityMap);

        verify(orderBookRepository, never()).bulkIncreaseStock(any());
        verify(orderBookSagaLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 증가 - 정상 처리")
    void increaseStock_Success() {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 1);

        when(orderBookSagaLogRepository.existsById(any(OrderBookSagaLogId.class))).thenReturn(false);

        orderBookService.increaseStock(sagaId, quantityMap);

        verify(orderBookRepository).bulkIncreaseStock(quantityMap);
        verify(orderBookSagaLogRepository).save(any(OrderBookSagaLog.class));
    }

    @Test
    @DisplayName("재고 롤백 - 이미 처리된 사가")
    void rollbackStock_AlreadyProcessed() {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 1);

        when(orderBookSagaLogRepository.existsById(any(OrderBookSagaLogId.class))).thenReturn(true);

        orderBookService.rollbackStock(sagaId, quantityMap);

        verify(orderBookRepository, never()).bulkIncreaseStock(any());
        verify(orderBookSagaLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 롤백 - 감소 이력 없음")
    void rollbackStock_NoDecreaseHistory() {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 1);

        // First check returns false (not processed rollback)
        when(orderBookSagaLogRepository.existsById(argThat(id -> id != null && id.sagaType() == OrderSagaType.ROLLBACK_STOCK)))
                .thenReturn(false);
        // Second check returns false (no decrease history)
        when(orderBookSagaLogRepository.existsById(argThat(id -> id != null && id.sagaType() == OrderSagaType.DECREASE_STOCK)))
                .thenReturn(false);

        orderBookService.rollbackStock(sagaId, quantityMap);

        verify(orderBookRepository, never()).bulkIncreaseStock(any());
        verify(orderBookSagaLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 롤백 - 정상 처리")
    void rollbackStock_Success() {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 1);

        when(orderBookSagaLogRepository.existsById(argThat(id -> id != null && id.sagaType() == OrderSagaType.ROLLBACK_STOCK)))
                .thenReturn(false);
        when(orderBookSagaLogRepository.existsById(argThat(id -> id != null && id.sagaType() == OrderSagaType.DECREASE_STOCK)))
                .thenReturn(true);

        orderBookService.rollbackStock(sagaId, quantityMap);

        verify(orderBookRepository).bulkIncreaseStock(quantityMap);
        verify(orderBookSagaLogRepository).save(any(OrderBookSagaLog.class));
    }
}
