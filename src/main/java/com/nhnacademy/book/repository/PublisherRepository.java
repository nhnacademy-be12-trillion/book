package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    Publisher findByPublisherName(String publisherName);
}
