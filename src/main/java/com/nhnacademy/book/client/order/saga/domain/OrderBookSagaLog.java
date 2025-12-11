package com.nhnacademy.book.client.order.saga.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class OrderBookSagaLog {
    // 복합 키
    @EmbeddedId
    private OrderBookSagaLogId id;

    private LocalDateTime processedAt;

    public OrderBookSagaLog(OrderBookSagaLogId id) {
        this.id = id;
        processedAt = LocalDateTime.now();
    }
}
