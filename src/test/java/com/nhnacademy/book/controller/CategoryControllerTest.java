package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.category.BookWithCategory;
import com.nhnacademy.book.dto.category.CategoryTreeResponse;
import com.nhnacademy.book.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 트리 구조 전체 조회 성공")
    void getAllCategories_Success() throws Exception {
        // given
        // 자식 카테고리 생성 (children이 없는 경우 빈 리스트)
        CategoryTreeResponse child = new CategoryTreeResponse(2L, "국내소설", Collections.emptyList());
        // 부모 카테고리 생성 (자식을 포함)
        CategoryTreeResponse root = new CategoryTreeResponse(1L, "도서", List.of(child));

        // 서비스 호출 시 위에서 만든 트리 반환
        given(categoryService.getCategoryTree()).willReturn(List.of(root));

        // when & then
        mockMvc.perform(get("/books/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1)) // 루트 카테고리 개수
                .andExpect(jsonPath("$[0].categoryName").value("도서")) // 루트 확인
                .andExpect(jsonPath("$[0].children[0].categoryName").value("국내소설")); // 자식 확인
    }

    @Test
    @DisplayName("특정 도서들의 카테고리 정보 조회 성공")
    void getBookCategories_Success() throws Exception {
        // given
        List<Long> bookIds = List.of(100L, 200L);

        // BookWithCategory 레코드 생성
        BookWithCategory book1 = new BookWithCategory(100L, "ISBN-100", 10, 15000, 5L);
        BookWithCategory book2 = new BookWithCategory(200L, "ISBN-200", 5, 20000, 6L);

        // 서비스가 리스트를 반환하면, 컨트롤러가 내부적으로 Response DTO로 변환함
        given(categoryService.getCategoryIds(anyList())).willReturn(List.of(book1, book2));

        // when & then
        mockMvc.perform(get("/books/categories")
                        .param("bookIds", "100,200") // 쿼리 파라미터 전달
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                // 첫 번째 책 검증
                .andExpect(jsonPath("$[0].bookId").value(100))
                .andExpect(jsonPath("$[0].isbn").value("ISBN-100"))
                .andExpect(jsonPath("$[0].bookStock").value(10))
                .andExpect(jsonPath("$[0].bookSalePrice").value(15000))
                .andExpect(jsonPath("$[0].categoryIds[0]").value(5)) // 리스트 형태의 ID 확인
                // 두 번째 책 검증
                .andExpect(jsonPath("$[1].bookId").value(200))
                .andExpect(jsonPath("$[1].isbn").value("ISBN-200"));
    }

}