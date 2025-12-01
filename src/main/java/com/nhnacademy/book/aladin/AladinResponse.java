package com.nhnacademy.book.aladin;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.print.DocFlavor;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinResponse(
        List<Item> item
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String title,
            String author,
            String pubDate,
            String description,

            @JsonAlias("isbn13")
            String isbn,

            int priceStandard,
            //int priceSales,
            String cover,
            String publisher,

            SubInfo subInfo
    ){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubInfo(
            String toc, // 목차
            String subTitle,
            String originalTitle
    ){}
}
