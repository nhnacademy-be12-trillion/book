package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("태그 저장 및 조회 테스트")
    void saveAndFindTag() {
        // given
        Tag newTag = new Tag("Spring Boot");

        // when
        Tag savedTag = tagRepository.save(newTag);

        // then
        assertThat(savedTag.getTagId()).isNotNull(); // ID 자동 생성 확인
        assertThat(savedTag.getTagName()).isEqualTo("Spring Boot");
    }

    @Test
    @DisplayName("태그 이름으로 조회 - 존재하는 경우")
    void findByTagName_ShouldReturnTag() {
        // given
        Tag tag = new Tag("JPA");
        entityManager.persist(tag); // DB에 미리 저장
        entityManager.flush();

        // when
        Optional<Tag> foundTag = tagRepository.findByTagName("JPA");

        // then
        assertThat(foundTag).isPresent();
        assertThat(foundTag.get().getTagName()).isEqualTo("JPA");
    }

    @Test
    @DisplayName("태그 이름으로 조회 - 존재하지 않는 경우")
    void findByTagName_ShouldReturnEmpty() {
        // given
        // 아무것도 저장하지 않음

        // when
        Optional<Tag> foundTag = tagRepository.findByTagName("Python");

        // then
        assertThat(foundTag).isEmpty();
    }

    @Test
    @DisplayName("중복된 태그 이름 저장 시 예외 발생 (Unique Constraint)")
    void saveDuplicateTagName_ShouldThrowException() {
        // given
        Tag tag1 = new Tag("Java");
        tagRepository.save(tag1);

        // when & then
        // 똑같은 "Java" 이름으로 저장 시도
        Tag tag2 = new Tag("Java");

        assertThatThrownBy(() -> tagRepository.saveAndFlush(tag2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}