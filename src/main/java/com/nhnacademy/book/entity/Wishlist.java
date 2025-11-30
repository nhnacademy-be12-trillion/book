package com.nhnacademy.book.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "Wishlist",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "book_id"})
        })
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public static Wishlist create(Member member, Book book) {
        return Wishlist.builder()
                .member(member)
                .book(book)
                .build();
    }
}
