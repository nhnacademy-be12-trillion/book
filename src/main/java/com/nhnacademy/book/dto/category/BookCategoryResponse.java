package com.nhnacademy.book.dto.category;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record BookCategoryResponse(Long bookId, String isbn, Integer bookStock, Integer bookSalePrice, List<Long> categoryIds) {
    public static List<BookCategoryResponse> of(List<BookWithCategory> bookWithCategories) {
        Map<Long, BookWithCategory> categories = bookWithCategories.stream()
                .collect(Collectors.toMap(BookWithCategory::bookId,book->book));

        Map<Long, Set<Long>> categoryIds = bookWithCategories.stream()
                .collect(
                        Collectors.groupingBy(
                                BookWithCategory::bookId,
                                Collectors.mapping(BookWithCategory::categoryId, Collectors.toSet())
                        ));

        return categories.keySet()
                .stream()
                .map(bookId->{
                    BookWithCategory book= categories.get(bookId);
                    return new BookCategoryResponse(bookId, book.isbn(),book.bookStock(),book.bookSalePrice(),categoryIds.get(bookId).stream().toList());
                })
                .toList();
    }
}
