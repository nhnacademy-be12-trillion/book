package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import com.nhnacademy.book.repository.BookFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @InjectMocks
    private FileServiceImpl fileService;

    @Mock
    private BookFileRepository bookFileRepository;

    @Test
    @DisplayName("도서 이미지 저장 - 기존 이미지가 없으면 새로 저장")
    void saveBookImage_New() {
        // given
        Long bookId = 1L;
        String newUrl = "http://new.image.com";

        // 기존 파일 없음
        given(bookFileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK))
                .willReturn(Optional.empty());

        // when
        fileService.saveBookImage(bookId, newUrl);

        // then
        // delete는 호출되지 않아야 함
        verify(bookFileRepository, never()).delete(any());
        // save는 호출되어야 함
        verify(bookFileRepository).save(any(BookFile.class));
    }

    @Test
    @DisplayName("도서 이미지 저장 - 기존 이미지가 있으면 삭제 후 저장 (덮어쓰기)")
    void saveBookImage_Update() {
        // given
        Long bookId = 1L;
        String newUrl = "http://new.image.com";
        BookFile oldFile = BookFile.builder()
                .fileUrl("http://old.image.com")
                .fileType(FileType.BOOK)
                .joinedId(bookId)
                .build();

        // 기존 파일 있음
        given(bookFileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK))
                .willReturn(Optional.of(oldFile));

        // when
        fileService.saveBookImage(bookId, newUrl);

        // then
        // 기존 파일 삭제 호출 확인
        verify(bookFileRepository).delete(oldFile);
        // 새 파일 저장 호출 확인
        verify(bookFileRepository).save(any(BookFile.class));
    }

    @Test
    @DisplayName("리뷰 이미지 저장 - 리스트가 정상적일 때")
    void saveReviewImages_Success() {
        // given
        Long reviewId = 100L;
        List<String> urls = List.of("url1", "url2");

        // when
        fileService.saveReviewImages(reviewId, urls);

        // then
        // saveAll이 호출되었는지 확인
        verify(bookFileRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("리뷰 이미지 저장 - 리스트가 비어있거나 Null이면 아무것도 안 함")
    void saveReviewImages_EmptyOrNull() {
        // given
        Long reviewId = 100L;

        // when
        fileService.saveReviewImages(reviewId, null);
        fileService.saveReviewImages(reviewId, Collections.emptyList());

        // then
        // saveAll이 절대 호출되면 안 됨
        verify(bookFileRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("도서 이미지 조회 - 이미지가 있을 때")
    void getBookImage_Found() {
        // given
        Long bookId = 1L;
        String url = "http://image.com";
        BookFile file = BookFile.builder().fileUrl(url).build();

        given(bookFileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK))
                .willReturn(Optional.of(file));

        // when
        String result = fileService.getBookImage(bookId);

        // then
        assertThat(result).isEqualTo(url);
    }

    @Test
    @DisplayName("도서 이미지 조회 - 이미지가 없을 때 Null 반환")
    void getBookImage_NotFound() {
        // given
        Long bookId = 1L;
        given(bookFileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK))
                .willReturn(Optional.empty());

        // when
        String result = fileService.getBookImage(bookId);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("리뷰 이미지 리스트 조회 - 파일이 있을 때")
    void getReviewImages_Found() {
        // given
        Long reviewId = 100L;
        BookFile file1 = BookFile.builder().fileUrl("url1").build();
        BookFile file2 = BookFile.builder().fileUrl("url2").build();

        given(bookFileRepository.findAllByJoinedIdAndFileType(reviewId, FileType.REVIEW))
                .willReturn(List.of(file1, file2));

        // when
        List<String> results = fileService.getReviewImages(reviewId);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly("url1", "url2");
    }

    @Test
    @DisplayName("리뷰 이미지 리스트 조회 - 파일이 없을 때 빈 리스트 반환")
    void getReviewImages_Empty() {
        // given
        Long reviewId = 100L;
        given(bookFileRepository.findAllByJoinedIdAndFileType(reviewId, FileType.REVIEW))
                .willReturn(Collections.emptyList());

        // when
        List<String> results = fileService.getReviewImages(reviewId);

        // then
        assertThat(results).isEmpty();
    }
}