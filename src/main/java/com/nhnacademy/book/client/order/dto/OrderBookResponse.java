package com.nhnacademy.book.client.order.dto;

import java.util.Set;

public record OrderBookResponse(
    Long bookId,
    String bookName,
    Set<Long> categoryIds,
    int price,
    boolean canPackage,
    String imageUrl
) {
    public static OrderBookResponse create(OrderBook orderBook, Set<Long> categoryIds) {
        return new OrderBookResponse(
            orderBook.bookId(),
            orderBook.bookName(),
            categoryIds,
            orderBook.price(),
            orderBook.canPackage(),
            orderBook.imageUrl()
        );
    }
}
