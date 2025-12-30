package com.nhnacademy.book.controller;

import com.nhnacademy.book.dto.category.CategorySearchResponse;
import com.nhnacademy.book.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // [중요] 새로 바뀐 패키지
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryAdminController.class) // 컨트롤러 테스트 환경 로드
class CategoryAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 검색 성공")
    void searchCategories_Success() throws Exception {
        // given
        String keyword = "소설";
        List<CategorySearchResponse> responseList = List.of(
                new CategorySearchResponse(1L, "국내소설"),
                new CategorySearchResponse(2L, "해외소설")
        );

        // Mocking: 서비스가 호출되면 미리 준비한 리스트 반환
        given(categoryService.searchCategories(keyword)).willReturn(responseList);

        // when & then
        mockMvc.perform(get("/admin/categories/search")
                        .param("keyword", keyword)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].categoryName").value("국내소설"))
                .andExpect(jsonPath("$[1].categoryName").value("해외소설"));
    }

    @Test
    @DisplayName("검색 결과 없음 - 빈 리스트 반환")
    void searchCategories_Empty() throws Exception {
        // given
        String keyword = "없는키워드";
        given(categoryService.searchCategories(keyword)).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/admin/categories/search")
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    @DisplayName("파라미터 누락 시 400 Bad Request")
    void searchCategories_BadRequest() throws Exception {
        // keyword 파라미터 없이 호출
        mockMvc.perform(get("/admin/categories/search"))
                .andExpect(status().isBadRequest());
    }
}