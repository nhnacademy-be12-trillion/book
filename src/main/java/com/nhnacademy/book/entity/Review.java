package com.nhnacademy.book.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private int reviewRate;

    @Lob
    private String reviewContents;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void update(int reviewRate, String reviewContents) {
        this.reviewRate = reviewRate;
        this.reviewContents = reviewContents;
        this.updatedAt = LocalDateTime.now();
    }
}