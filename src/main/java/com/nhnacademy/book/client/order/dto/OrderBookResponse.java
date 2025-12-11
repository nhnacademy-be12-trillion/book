package com.nhnacademy.book.client.order.dto;

public record OrderBookResponse(
    Long bookId,
    String bookName,
    int price,
    boolean canPackage,
    String imageUrl
) {}
