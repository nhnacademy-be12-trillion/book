package com.nhnacademy.book.client.order.saga.domain;

public enum OrderSagaType {
    DECREASE_STOCK, // 재고 감소 (주문 생성)
    INCREASE_STOCK, // 재고 증가 (주문 취소 / 주문 상품 환불)
    ROLLBACK_STOCK  // 재고 복구 (주문 생성 실패 시 보상)
}
