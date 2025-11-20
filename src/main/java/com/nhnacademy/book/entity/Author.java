package com.nhnacademy.book.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@NoArgsConstructor
@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authorId;

    @Lob
    private String authorName;

    public Author (String authorName) {
        this.authorName = authorName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        return Objects.equals(authorName, author.authorName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(authorName);
    }
}
