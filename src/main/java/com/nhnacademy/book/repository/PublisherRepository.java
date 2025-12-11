package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    Optional<Publisher> findByPublisherName(String publisherName);
}
