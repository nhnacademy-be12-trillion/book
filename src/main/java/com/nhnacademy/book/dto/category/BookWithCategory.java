package com.nhnacademy.book.dto.category;

public record BookWithCategory(Long bookId, String isbn, Integer bookStock, Integer bookSalePrice, Long categoryId) {
}
