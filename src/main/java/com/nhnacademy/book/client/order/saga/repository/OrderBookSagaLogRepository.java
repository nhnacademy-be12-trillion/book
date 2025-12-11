package com.nhnacademy.book.client.order.saga.repository;

import com.nhnacademy.book.client.order.saga.domain.OrderBookSagaLog;
import com.nhnacademy.book.client.order.saga.domain.OrderBookSagaLogId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderBookSagaLogRepository extends JpaRepository<OrderBookSagaLog, OrderBookSagaLogId> {
}
