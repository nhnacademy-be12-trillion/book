package com.nhnacademy.book.client.order.saga.domain;


import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.UUID;

// 주문 생성 흐름: 재고 감소 -> 쿠폰 사용 -> 포인트 사용 / 도중에 오류 발생시 역순으로 보상 트랜잭션 시작
// 주문 생성 사가 과정 중 쿠폰 사용 도중에 오류 발생 (재고는 감소된 상태) -> 동일한 사가로 바로 보상 트랜잭션이 시작됨(재고 증가 시도) -> 이미 해당 사가 ID가 DB에 저장되어 있어 재고 증가 실패
// 따라서 복합 키로 설계
@Embeddable
public record OrderBookSagaLogId(
    UUID sagaId,

    @Enumerated(EnumType.STRING)
    OrderSagaType sagaType
) {}
