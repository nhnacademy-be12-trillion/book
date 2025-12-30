package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.category.BookWithCategory;
import com.nhnacademy.book.dto.category.CategorySearchResponse;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.entity.Category;
import com.nhnacademy.book.repository.BookCategoryRepository;
import com.nhnacademy.book.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookCategoryRepository bookCategoryRepository;

    @Test
    @DisplayName("카테고리 트리 조회 성공 - 계층 구조가 잘 변환되는지 확인")
    void getCategoryTree_Success() {
        // given
        // 자식 카테고리 생성
        Category child = new Category(2L, "국내소설");
        ReflectionTestUtils.setField(child, "children", new ArrayList<>()); // 자식의 자식은 없음 (Null 방지)

        // 부모 카테고리 생성 및 자식 연결
        Category root = new Category(1L, "도서");
        ReflectionTestUtils.setField(root, "children", List.of(child));

        // 최상위 카테고리 조회 시 root 반환
        given(categoryRepository.findAllByParentIsNull()).willReturn(List.of(root));

        // when
        List<CategoryTreeResponse> result = categoryService.getCategoryTree();

        // then
        assertThat(result).hasSize(1);

        // 루트 노드 검증
        CategoryTreeResponse rootDto = result.get(0);
        assertThat(rootDto.categoryId()).isEqualTo(1L);
        assertThat(rootDto.categoryName()).isEqualTo("도서");

        // 자식 노드 검증
        assertThat(rootDto.children()).hasSize(1);
        assertThat(rootDto.children().get(0).categoryId()).isEqualTo(2L);
        assertThat(rootDto.children().get(0).categoryName()).isEqualTo("국내소설");
    }

    @Test
    @DisplayName("도서 ID 리스트로 카테고리 매핑 정보 조회 성공")
    void getCategoryIds_Success() {
        // given
        List<Long> bookIds = List.of(100L, 200L);
        BookWithCategory dto1 = new BookWithCategory(100L, "ISBN1", 10, 1000, 1L);
        BookWithCategory dto2 = new BookWithCategory(200L, "ISBN2", 20, 2000, 2L);

        given(bookCategoryRepository.findBookCategoryIds(bookIds)).willReturn(List.of(dto1, dto2));

        // when
        List<BookWithCategory> result = categoryService.getCategoryIds(bookIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).bookId()).isEqualTo(100L);
        assertThat(result.get(1).bookId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("카테고리 검색 성공 - 키워드가 포함된 카테고리 반환")
    void searchCategories_Success() {
        // given
        String keyword = "소설";
        Category category = new Category(1L, "국내소설");

        given(categoryRepository.findTop10ByCategoryNameContaining(keyword))
                .willReturn(List.of(category));

        // when
        List<CategorySearchResponse> result = categoryService.searchCategories(keyword);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryName()).isEqualTo("국내소설");
    }

    @Test
    @DisplayName("카테고리 검색 - 키워드가 Null이거나 빈 문자열이면 빈 리스트 반환 (DB 호출 X)")
    void searchCategories_NullOrBlank_ShouldReturnEmpty() {
        // given
        String nullKeyword = null;
        String blankKeyword = "   ";

        // when
        List<CategorySearchResponse> resultNull = categoryService.searchCategories(nullKeyword);
        List<CategorySearchResponse> resultBlank = categoryService.searchCategories(blankKeyword);

        // then
        assertThat(resultNull).isEmpty();
        assertThat(resultBlank).isEmpty();

        // DB 조회를 아예 안 했는지 검증
        verify(categoryRepository, never()).findTop10ByCategoryNameContaining(anyString());
    }

    @Test
    @DisplayName("카테고리 검색 - 결과가 없을 때 빈 리스트 반환")
    void searchCategories_NoResult() {
        // given
        String keyword = "없는 카테고리";
        given(categoryRepository.findTop10ByCategoryNameContaining(keyword)).willReturn(Collections.emptyList());

        // when
        List<CategorySearchResponse> result = categoryService.searchCategories(keyword);

        // then
        assertThat(result).isEmpty();
    }
}