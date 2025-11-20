package com.nhnacademy.book.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@NoArgsConstructor
@Entity
public class Publisher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long publisherId;

    private String publisherName;

    public Publisher(String publisherName) {
        this.publisherName = publisherName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Publisher publisher = (Publisher) o;
        return Objects.equals(publisherName, publisher.publisherName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publisherName);
    }
}
