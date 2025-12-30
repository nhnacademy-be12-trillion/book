package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("최상위 카테고리(부모가 없는) 목록 조회")
    void findAllByParentIsNull() {
        // given
        // 루트 카테고리 생성 (Parent = null)
        Category root1 = new Category(1L, "국내도서");
        Category root2 = new Category(2L, "외국도서");

        // 자식 카테고리 생성 (Parent = root1)
        Category child1 = new Category(3L, "소설");
        child1.setParent(root1);

        // 자식의 자식 카테고리 생성 (Parent = child1)
        Category grandChild = new Category(4L, "판타지");
        grandChild.setParent(child1);

        categoryRepository.saveAll(List.of(root1, root2, child1, grandChild));

        // when
        List<Category> results = categoryRepository.findAllByParentIsNull();

        // then
        // 부모가 null인 root1, root2만 조회되어야 함
        assertThat(results).hasSize(2);
        assertThat(results).extracting("categoryName")
                .containsExactlyInAnyOrder("국내도서", "외국도서");

        assertThat(results).extracting("categoryId")
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("카테고리 이름 검색 (키워드 포함) - 상위 10개 제한 확인")
    void findTop10ByCategoryNameContaining() {
        // given
        // "Java"가 포함된 카테고리 15개 생성
        for (long i = 1; i <= 15; i++) {
            Category category = new Category(i, "Java Programming " + i);
            categoryRepository.save(category);
        }

        // "Python" 카테고리 1개 생성 (검색되지 말아야 함)
        Category otherCategory = new Category(100L, "Python Programming");
        categoryRepository.save(otherCategory);

        // when
        List<Category> results = categoryRepository.findTop10ByCategoryNameContaining("Java");

        // then
        // 개수는 10개여야 함 (Top 10)
        assertThat(results).hasSize(10);

        // 검색된 모든 항목은 이름에 "Java"를 포함해야 함
        assertThat(results).allMatch(c -> c.getCategoryName().contains("Java"));

        // "Python"은 포함되지 않아야 함
        assertThat(results).extracting("categoryName")
                .doesNotContain("Python Programming");
    }

    @Test
    @DisplayName("카테고리 이름 검색 - 결과가 없는 경우")
    void findTop10ByCategoryNameContaining_Empty() {
        // given
        Category category = new Category(1L, "역사");
        categoryRepository.save(category);

        // when
        List<Category> results = categoryRepository.findTop10ByCategoryNameContaining("과학");

        // then
        assertThat(results).isEmpty();
    }
}