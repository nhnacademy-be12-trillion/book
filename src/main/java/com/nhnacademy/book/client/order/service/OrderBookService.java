package com.nhnacademy.book.client.order.service;

import com.nhnacademy.book.client.order.dto.OrderBookResponse;
import com.nhnacademy.book.client.order.repository.OrderBookRepository;
import com.nhnacademy.book.client.order.saga.domain.OrderBookSagaLog;
import com.nhnacademy.book.client.order.saga.domain.OrderBookSagaLogId;
import com.nhnacademy.book.client.order.saga.domain.OrderSagaType;
import com.nhnacademy.book.client.order.saga.repository.OrderBookSagaLogRepository;
import com.nhnacademy.book.entity.FileType;
import com.nhnacademy.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OrderBookService {
    private final BookRepository bookRepository;
    private final OrderBookSagaLogRepository orderBookSagaLogRepository;
    private final OrderBookRepository orderBookRepository;

    // 도서 정보 조회 -> 멱등성이 보장되므로 사가 ID 불필요
    @Transactional(readOnly = true)
    public List<OrderBookResponse> getAllBookByBookIds(List<Long> bookIds) {
        return bookRepository.findBooksInfoForOrderByIds(bookIds, FileType.BOOK);
    }

    // 주문 생성을 위한 재고 감소 메서드
    @Transactional
    public void decreaseStock(UUID sagaId, Map<Long, Integer> quantityMap) {
        OrderBookSagaLogId sagaLogId = new OrderBookSagaLogId(sagaId, OrderSagaType.DECREASE_STOCK);
        OrderBookSagaLog sagaLog = new OrderBookSagaLog(sagaLogId);

        // 1. 이미 처리된 작업이라면 즉시 리턴 (에러가 아님)
        if (orderBookSagaLogRepository.existsById(sagaLogId)) {
            return;
        }

        // 2. Bulk-Update 로직 호출
        orderBookRepository.decreaseStock(quantityMap);

        // 3. 사가 ID를 DB에 저장해 멱등성 보장
        orderBookSagaLogRepository.save(sagaLog);
    }

    // 주문 취소 / 주문 상품 환불을 위한 재고 증가 메서드
    @Transactional
    public void increaseStock(UUID sagaId, Map<Long, Integer> quantityMap) {
        OrderBookSagaLogId sagaLogId = new OrderBookSagaLogId(sagaId, OrderSagaType.INCREASE_STOCK);
        OrderBookSagaLog sagaLog = new OrderBookSagaLog(sagaLogId);

        // 이미 처리된 작업이라면 즉시 리턴 (에러가 아님)
        if (orderBookSagaLogRepository.existsById(sagaLogId)) {
            return;
        }
        
        orderBookRepository.bulkIncreaseStock(quantityMap);

        // 사가 ID를 DB에 저장해 멱등성 보장
        orderBookSagaLogRepository.save(sagaLog);
    }

    // 주문 생성 실패 시 실행될 보상 트랜잭션을 위한 재고 복구 메서드
    @Transactional
    public void rollbackStock(UUID sagaId, Map<Long, Integer> quantityMap) {
        OrderBookSagaLogId sagaLogId = new OrderBookSagaLogId(sagaId, OrderSagaType.ROLLBACK_STOCK);
        OrderBookSagaLog sagaLog = new OrderBookSagaLog(sagaLogId);

        // 이미 처리된 작업이라면 즉시 리턴 (에러가 아님)
        if (orderBookSagaLogRepository.existsById(sagaLogId)) {
            return;
        }

        // 해당 사가에 의해 재고가 감소된 적이 있는지 확인
        boolean hasDecreased = orderBookSagaLogRepository.existsById(new OrderBookSagaLogId(sagaId, OrderSagaType.DECREASE_STOCK));

        // 만약 재고가 감소된 적이 없다면 즉시 리턴 (재고가 감소되지 않았으므로 증가하면 안 됨)
        if (!hasDecreased) {
            return;
        }
        
        orderBookRepository.bulkIncreaseStock(quantityMap);

        // 사가 ID를 DB에 저장해 멱등성 보장
        orderBookSagaLogRepository.save(sagaLog);
    }
}
