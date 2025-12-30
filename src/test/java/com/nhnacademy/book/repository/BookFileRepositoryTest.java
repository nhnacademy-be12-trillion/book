package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookFileRepositoryTest {

    @Autowired
    private BookFileRepository bookFileRepository;

    @Test
    @DisplayName("도서 ID와 파일 타입으로 첫 번째 이미지 조회 (썸네일용)")
    void findFirstByJoinedIdAndFileType() {
        // given
        Long bookId = 1L;
        BookFile file1 = BookFile.builder()
                .fileUrl("book-image-1.jpg")
                .fileType(FileType.BOOK)
                .joinedId(bookId)
                .build();

        BookFile file2 = BookFile.builder()
                .fileUrl("book-image-2.jpg") // 나중에 등록된 파일
                .fileType(FileType.BOOK)
                .joinedId(bookId)
                .build();

        bookFileRepository.save(file1);
        bookFileRepository.save(file2);

        // when
        Optional<BookFile> result = bookFileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK);

        // then
        assertThat(result).isPresent();
        // 순서 보장이 필요한 경우 엔티티에 생성시간 필드가 있거나 OrderBy가 필요하지만,
        // 여기서는 "값이 조회되는지"를 중점적으로 확인
        assertThat(result.get().getJoinedId()).isEqualTo(bookId);
        assertThat(result.get().getFileType()).isEqualTo(FileType.BOOK);
    }

    @Test
    @DisplayName("리뷰 ID와 파일 타입으로 모든 이미지 조회")
    void findAllByJoinedIdAndFileType() {
        // given
        Long reviewId = 100L;
        BookFile reviewImage1 = new BookFile("review-1.jpg", FileType.REVIEW, reviewId);
        BookFile reviewImage2 = new BookFile("review-2.jpg", FileType.REVIEW, reviewId);

        // 다른 리뷰의 이미지 (조회되면 안 됨)
        BookFile otherReviewImage = new BookFile("other.jpg", FileType.REVIEW, 999L);
        // 같은 ID지만 다른 타입 (조회되면 안 됨 - 예: 도서 이미지)
        BookFile bookImage = new BookFile("book.jpg", FileType.BOOK, reviewId);

        bookFileRepository.saveAll(List.of(reviewImage1, reviewImage2, otherReviewImage, bookImage));

        // when
        List<BookFile> results = bookFileRepository.findAllByJoinedIdAndFileType(reviewId, FileType.REVIEW);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting("fileUrl")
                .containsExactlyInAnyOrder("review-1.jpg", "review-2.jpg");
    }

    @Test
    @DisplayName("여러 연결 ID(List)에 해당하는 파일 목록 조회")
    void findAllByJoinedIdInAndFileType() {
        // given
        BookFile book1Image = new BookFile("book1.jpg", FileType.BOOK, 1L);
        BookFile book2Image = new BookFile("book2.jpg", FileType.BOOK, 2L);
        BookFile book3Image = new BookFile("book3.jpg", FileType.BOOK, 3L); // 조회 대상 아님

        bookFileRepository.saveAll(List.of(book1Image, book2Image, book3Image));

        List<Long> targetIds = List.of(1L, 2L);

        // when
        List<BookFile> results = bookFileRepository.findAllByJoinedIdInAndFileType(targetIds, FileType.BOOK);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting("joinedId")
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(results).extracting("fileUrl")
                .contains("book1.jpg", "book2.jpg")
                .doesNotContain("book3.jpg");
    }
}