package com.nhnacademy.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.impl.BookAiRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBookController.class)
class AdminBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private BookAiRegistrationService bookAiRegistrationService;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    // BookCreateRequest 더미 데이터 생성
    private BookCreateRequest createBookCreateRequest() {
        return new BookCreateRequest(
                "978-1234567890",   // isbn
                "테스트 책",        // bookName
                "설명입니다.",      // bookDescription
                "테스트 출판사",    // bookPublisher
                "테스트 작가",      // bookAuthor
                "태그1,태그2",      // tags (String)
                List.of(1L, 2L),    // categoryIdList
                LocalDate.now(),    // bookPublicationDate
                "목차입니다.",      // bookIndex
                true,               // bookPackaging
                BookState.ON_SALE,  // bookState (Enum)
                100,                // bookStock
                20000,              // bookRegularPrice
                18000,              // bookSalePrice
                null                // bookImage (String)
        );
    }

    @Test
    @DisplayName("도서 등록 - 성공 (이미지 파일 포함)")
    void createBook() throws Exception {
        // given
        BookCreateRequest request = createBookCreateRequest();
        String requestJson = objectMapper.writeValueAsString(request);
        Long createdBookId = 100L;

        // JSON Part ("book")
        MockMultipartFile bookPart = new MockMultipartFile(
                "book",
                "",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // File Part ("file")
        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "cover.jpg",
                "image/jpeg",
                "dummy image data".getBytes()
        );

        given(bookService.createBook(any(BookCreateRequest.class), any(MultipartFile.class)))
                .willReturn(createdBookId);

        // when & then
        mockMvc.perform(multipart("/admin/books")
                        .file(bookPart)
                        .file(filePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().string(String.valueOf(createdBookId)))
                .andDo(print());
    }

    @Test
    @DisplayName("도서 등록 - 성공 (이미지 파일 없음)")
    void createBookWithoutFile() throws Exception {
        // given
        BookCreateRequest request = createBookCreateRequest();
        String requestJson = objectMapper.writeValueAsString(request);
        Long createdBookId = 101L;

        MockMultipartFile bookPart = new MockMultipartFile(
                "book",
                "",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // 파일 없이 호출 시 Service에는 null이 전달돰
        given(bookService.createBook(any(BookCreateRequest.class), eq(null)))
                .willReturn(createdBookId);

        // when & then
        mockMvc.perform(multipart("/admin/books")
                        .file(bookPart)
                        // filePart 없음
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().string(String.valueOf(createdBookId)))
                .andDo(print());
    }

    @Test
    @DisplayName("도서 수정")
    void updateBook() throws Exception {
        // given
        Long bookId = 1L;
        // BookUpdateRequest 필드는 가정하여 작성
        BookUpdateRequest updateRequest = new BookUpdateRequest(
                "수정된 책 이름",
                "수정된 설명",
                "수정 목차",
                false,
                BookState.SOLD_OUT,
                50,
                15000,
                "http://new.image.url"
        );

        // when & then
        mockMvc.perform(put("/admin/books/{book-id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNoContent()) // 204 No Content
                .andDo(print());

        // verify
        verify(bookService).updateBook(eq(bookId), any(BookUpdateRequest.class));
    }

    @Test
    @DisplayName("도서 삭제")
    void deleteBook() throws Exception {
        // given
        Long bookId = 1L;

        // when & then
        mockMvc.perform(delete("/admin/books/{book-id}", bookId))
                .andExpect(status().isNoContent()) // 204 No Content
                .andDo(print());

        // verify
        verify(bookService).deleteBook(bookId);
    }

    @Test
    @DisplayName("ISBN으로 도서 정보 조회 (AI 연동)")
    void getBookInfoByIsbn() throws Exception {
        // given
        String isbn = "9781234567890";
        BookCreateRequest mockResponse = createBookCreateRequest();

        given(bookAiRegistrationService.getBookInfoByIsbn(isbn)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/admin/books/isbn/{isbn}", isbn)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockResponse)))
                .andDo(print());

        // verify
        verify(bookAiRegistrationService).getBookInfoByIsbn(isbn);
    }
}