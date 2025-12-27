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

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 주문당 1개 제한을 위한 주문 번호 필드
    // unique = true를 걸어서 혹시 모를 중복 저장 막기
    @Column(name = "order_id", unique = true)
    private Long orderId;

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