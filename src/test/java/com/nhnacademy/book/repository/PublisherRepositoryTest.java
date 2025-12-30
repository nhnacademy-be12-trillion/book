package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Publisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PublisherRepositoryTest {

    @Autowired
    private PublisherRepository publisherRepository;

    @Test
    @DisplayName("출판사 이름으로 조회 - 성공")
    void findByPublisherName() {
        // given
        String name = "NHN 출판사";
        Publisher publisher = new Publisher(name); // 생성자 사용
        publisherRepository.save(publisher);

        // when
        Optional<Publisher> foundPublisher = publisherRepository.findByPublisherName(name);

        // then
        assertThat(foundPublisher).isPresent(); // 값이 있는지 확인
        assertThat(foundPublisher.get().getPublisherName()).isEqualTo(name); // 이름 일치 확인
        assertThat(foundPublisher.get().getPublisherId()).isNotNull(); // ID 자동 생성 확인
    }

    @Test
    @DisplayName("출판사 이름으로 조회 - 존재하지 않는 경우")
    void findByPublisherName_NotFound() {
        // given
        String name = "없는 출판사";

        // when
        Optional<Publisher> foundPublisher = publisherRepository.findByPublisherName(name);

        // then
        assertThat(foundPublisher).isEmpty(); // 결과가 비어있어야 함
    }
}