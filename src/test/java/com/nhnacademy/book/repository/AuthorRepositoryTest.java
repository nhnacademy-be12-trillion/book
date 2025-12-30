package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Author;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthorRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    @DisplayName("저자 이름으로 조회 - 성공")
    void findByAuthorName() {
        // given
        String name = "김영한";
        Author author = new Author(name);
        authorRepository.save(author);

        // when
        Optional<Author> foundAuthor = authorRepository.findByAuthorName(name);

        // then
        assertThat(foundAuthor).isPresent(); // 값이 존재하는지 확인
        assertThat(foundAuthor.get().getAuthorName()).isEqualTo(name); // 이름이 일치하는지 확인
        assertThat(foundAuthor.get().getAuthorId()).isNotNull(); // ID가 자동생성 되었는지 확인
    }

    @Test
    @DisplayName("저자 이름으로 조회 - 존재하지 않는 이름")
    void findByAuthorName_NotFound() {
        // given
        String name = "존재하지 않는 작가";

        // when
        Optional<Author> foundAuthor = authorRepository.findByAuthorName(name);

        // then
        assertThat(foundAuthor).isEmpty(); // 결과가 비어있어야 함
    }
}