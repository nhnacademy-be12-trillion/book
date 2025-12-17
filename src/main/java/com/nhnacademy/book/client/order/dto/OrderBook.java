package com.nhnacademy.book.client.order.dto;

public record OrderBook(
    Long bookId,
    String bookName,
    int price,
    boolean canPackage,
    String imageUrl
) {}
