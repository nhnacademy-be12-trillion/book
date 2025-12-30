package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    // 출판사 이름으로 출판사 단건 조회
    Optional<Publisher> findByPublisherName(String publisherName);
}
