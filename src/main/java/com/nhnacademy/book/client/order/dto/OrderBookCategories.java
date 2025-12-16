package com.nhnacademy.book.client.order.dto;

import java.util.Set;

public record OrderBookCategories(
    Long bookId,
    Set<Long> categoryIds
) {}
