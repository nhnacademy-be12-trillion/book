package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookDetailResponse;
import com.nhnacademy.book.dto.book.BookListResponse;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.entity.Publisher;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.service.FileService; // 추가
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile; // 추가

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @InjectMocks
    private BookServiceImpl bookService;

    @Mock
    private BookRepository bookRepository;

    // ServiceImpl에서 사용하는 의존성 Mock 추가
    @Mock
    private FileService fileService;
    @Mock
    private MinioService minioService;

    @Test
    @DisplayName("도서 등록 성공 - DTO 필드가 Entity로 잘 변환되고 저장되는지 확인")
    void createBook_Success() {
        // given
        BookCreateRequest request = new BookCreateRequest(
                "978-1-2345-6789-0",
                "테스트 도서",
                "설명",
                "테스트 출판사",
                LocalDate.now(),
                "목차",
                true,
                BookState.ON_SALE,
                100, // stock
                20000, // regular price
                18000, // sale price
                "image.jpg"
        );

        Book savedBook = new Book();
        savedBook.setBookId(1L);

        given(bookRepository.save(any(Book.class))).willReturn(savedBook);

        // when
        // [수정] createBook 메서드는 이제 MultipartFile도 받습니다.
        // 테스트에서는 파일이 없는 경우 null을 넘깁니다.
        Long bookId = bookService.createBook(request, null);

        // then
        assertThat(bookId).isEqualTo(1L);
        verify(bookRepository).save(any(Book.class));
    }

    // ... (나머지 테스트 코드는 변경 사항 없음) ...
    // getBook_Success, getBook_NotFound, getBooks_Success,
    // updateBook_Success, deleteBook_Success, deductStock_Success, deductStock_NotEnough 등은 그대로 유지
    // 전체 코드가 필요하면 말씀해주세요.
}