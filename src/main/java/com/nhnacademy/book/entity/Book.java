package com.nhnacademy.book.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nhnacademy.book.exception.NotFoundBookIndexException;
import com.nhnacademy.book.parser.CustomDateConverter;
import com.nhnacademy.book.parser.CustomPriceConverter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.builder.ToStringExclude;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 기본 생성자 (외부 접근 차단)
@Entity
@Table(name = "Book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @CsvBindByName(column = "ISBN_THIRTEEN_NO")
    @Column(unique = true)
    private String isbn;

    @Lob
    @CsvBindByName(column = "TITLE_NM")
    private String bookName;

    @Lob
    @CsvBindByName(column = "BOOK_INTRCN_CN")
    private String bookDescription;

    @CsvCustomBindByName(column = "TWO_PBLICTE_DE", converter = CustomDateConverter.class)
    private LocalDate bookPublicationDate;

    @Lob
    private String bookIndex;

    private boolean bookPackaging;

    @Enumerated(EnumType.STRING)
    private BookState bookState;

    private int bookStock;

    @CsvCustomBindByName(column = "PRC_VALUE", converter = CustomPriceConverter.class)
    private int bookRegularPrice;

    private int bookSalePrice;

    @Setter
    private double bookReviewRate;

    // [변경] 이미지 컬럼 제거 (BookFile 테이블로 이관)
    // @CsvBindByName(column = "IMAGE_URL")
    // private String bookImage;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToStringExclude
    @JsonBackReference
    private Set<BookAuthor> bookAuthors = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToStringExclude
    @JsonBackReference
    private Set<BookCategory> bookCategories = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToStringExclude
    @JsonBackReference
    private Set<BookTag> bookTags = new HashSet<>();

    @Setter
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Publisher publisher;

    @Column(columnDefinition = "integer default 0", nullable = false)
    private int viewCount;

    // 빌더 패턴 생성자 (Setter 대신 사용)
    @Builder
    public Book(String isbn, String bookName, String bookDescription, LocalDate bookPublicationDate,
                String bookIndex, boolean bookPackaging, BookState bookState, int bookStock,
                int bookRegularPrice, int bookSalePrice, double bookReviewRate, Publisher publisher) {
        this.isbn = isbn;
        this.bookName = bookName;
        this.bookDescription = bookDescription;
        this.bookPublicationDate = bookPublicationDate;
        this.bookIndex = bookIndex;
        this.bookPackaging = bookPackaging;
        this.bookState = bookState;
        this.bookStock = bookStock;
        this.bookRegularPrice = bookRegularPrice;
        this.bookSalePrice = bookSalePrice;
        this.bookReviewRate = bookReviewRate;
        this.publisher = publisher;
    }

    // 도서 정보 수정 (Setter 대신 의미 있는 메서드 사용)
    public void updateBookInfo(String bookName, String bookDescription, String bookIndex,
                               boolean bookPackaging, BookState bookState, int bookStock, int bookSalePrice) {
        this.bookName = bookName;
        this.bookDescription = bookDescription;
        this.bookIndex = bookIndex;
        this.bookPackaging = bookPackaging;
        this.bookState = bookState;
        this.bookStock = bookStock;
        this.bookSalePrice = bookSalePrice;
    }

    // 연관관계 편의 메서드
    public void assignPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    // 재고 차감 로직
    public void deductStock(int quantity) {
        int restStock = this.bookStock - quantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        this.bookStock = restStock;

        if (this.bookStock == 0) {
            this.bookState = BookState.SOLD_OUT;
        }
    }

    public void updateBookIndex(String toc){
        if(Objects.isNull(toc)){
            throw new NotFoundBookIndexException("목차 정보 없음");
        }
        this.bookIndex = toc;
    }

    // 판매 종료 처리 (삭제 대신 사용)
    public void markAsSoldOut() {
        this.bookState = BookState.SALE_END;
    }
}