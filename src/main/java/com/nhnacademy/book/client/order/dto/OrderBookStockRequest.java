package com.nhnacademy.book.client.order.dto;

import java.util.Map;

public record OrderBookStockRequest(
    Map<Long, Integer> quantityMap
) {}
