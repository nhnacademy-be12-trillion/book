package com.nhnacademy.book.aladin;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.print.DocFlavor;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinResponse(
        List<Item> item
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String title, //제목
            String author, //작가
            String pubDate, //출판일
            String description, //설명

            @JsonProperty("isbn13")
            String isbn, //isbn

            int priceStandard, //정가
            //int priceSales,
            String cover, //책 표지
            String publisher, //출판사

            SubInfo subInfo // 목차, 부제목, 제목 포함
    ){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubInfo(
            String toc, // 목차
            String subTitle,
            String originalTitle
    ){}
}
