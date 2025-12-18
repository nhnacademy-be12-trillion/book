package com.nhnacademy.book.dto.book;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookCategory;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.entity.Category;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record BookDetailResponse(
        Long bookId,
        String isbn,
        String bookName,
        String bookAuthor,
        String bookDescription,
        String bookPublisher,
        LocalDate bookPublicationDate,
        String bookIndex,
        boolean bookPackaging,
        BookState bookState,
        int bookStock,
        int bookRegularPrice,
        int bookSalePrice,
        int discountRate,
        double bookReviewRate,
        String bookImage,
        int viewCount,
        // 카테고리 경로 (Root -> Leaf 순서)
        List<CategoryInfo> categoryPath

) {
    // 내부용 레코드: 카테고리 정보
    public record CategoryInfo(Long categoryId, String categoryName) {}

    public static BookDetailResponse from(Book book, String imageUrl) {

        // 1. 작가 이름 리스트 변환
        String authors = book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getAuthorName())
                .collect(Collectors.joining(", "));

        // 2. 할인율 계산
        int discountRate = 0;
        if (book.getBookRegularPrice() > 0) {
            discountRate = (int) Math.round(
                    (double) (book.getBookRegularPrice() - book.getBookSalePrice())
                            / book.getBookRegularPrice() * 100
            );
        }

        // [수정됨] 3. 카테고리 계층 구조 생성 (Set 대응)
        List<CategoryInfo> categoryPath = new ArrayList<>();

        // 책에 연결된 카테고리가 있다면, 하나를 가져와서 부모를 탐색
        if (book.getBookCategories() != null && !book.getBookCategories().isEmpty()) {

            // Set이므로 get(0) 대신 stream().findFirst() 사용
            Optional<BookCategory> firstBookCategory = book.getBookCategories().stream().findFirst();

            if (firstBookCategory.isPresent()) {
                Category currentCategory = firstBookCategory.get().getCategory();

                // 부모가 null이 아닐 때까지 위로 올라가며 리스트에 추가
                while (currentCategory != null) {
                    categoryPath.add(new CategoryInfo(
                            currentCategory.getCategoryId(),
                            currentCategory.getCategoryName()
                    ));
                    // 부모 카테고리로 이동 (Category 엔티티에 getParent()가 있어야 함)
                    currentCategory = currentCategory.getParent();
                }

                // 현재: [소설, 문학, 국내도서] -> 역순 정렬 -> [국내도서, 문학, 소설]
                Collections.reverse(categoryPath);
            }
        }

        return new BookDetailResponse(
                book.getBookId(),
                book.getIsbn(),
                book.getBookName(),
                authors.isEmpty() ? "작가 미상" : authors,
                book.getBookDescription(),
                book.getPublisher() != null ? book.getPublisher().getPublisherName() : "출판사 미상",
                book.getBookPublicationDate(),
                book.getBookIndex(),
                book.isBookPackaging(),
                book.getBookState(),
                book.getBookStock(),
                book.getBookRegularPrice(),
                book.getBookSalePrice(),
                discountRate,
                book.getBookReviewRate(),
                imageUrl,
                book.getViewCount(),
                categoryPath // 완성된 계층 경로 전달
        );
    }
}