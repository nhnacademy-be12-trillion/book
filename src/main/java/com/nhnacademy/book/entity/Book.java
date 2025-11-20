package com.nhnacademy.book.entity;

import com.nhnacademy.book.parser.CustomDateConverter;
import com.nhnacademy.book.parser.CustomPriceConverter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Book")
public class Book {

    // 도서 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    // ISBN 번호
    @CsvBindByName(column = "ISBN_THIRTEEN_NO")
    @Column(unique = true)
    private String isbn;

    // 책 제목
    @Lob
    @CsvBindByName(column = "TITLE_NM")
    private String bookName;

    // 책 설명
    @Lob
    @CsvBindByName(column = "BOOK_INTRCN_CN")
    private String bookDescription;

    // 출판사 -> 테이블 분리
    @CsvBindByName(column = "PUBLISHER_NM")
    private String bookPublisher;

    //출판 일시
    @CsvCustomBindByName(
            column = "TWO_PBLICTE_DE",
            converter = CustomDateConverter.class)
    private LocalDate bookPublicationDate;

    // 목차
    private String bookIndex;

    // 포장 여부
    private boolean bookPackaging;

    // 책 상태
    @Enumerated(EnumType.STRING)
    private BookState bookState;

    // 재고
    private int bookStock;

    // 정가
    @CsvCustomBindByName(
            column = "PRC_VALUE",
            converter = CustomPriceConverter.class // 가격 컨버터 지정
    )
    private int bookRegularPrice;

    // 판매가
    private int bookSalePrice;

    // 리뷰 점수
    private double bookReviewRate;

    // 책 이미지 -> 파일 테이블에 따로 빼기?
    @CsvBindByName(column = "IMAGE_URL")
    private String bookImage;
}
