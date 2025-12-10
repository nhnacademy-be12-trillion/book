package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookFileRepository extends JpaRepository<BookFile, Long> {
    //도서 1개의 이미지 --> 이거 재활용 할거면 도서 이미지도 여러개 할거라는 말?
    Optional<BookFile> findFirstByJoinedIdAndFileType(Long joinedId, FileType fileType);

    //도서 이미지 1개만 할거라면? -> 그냥 위에 메서드 그냥 joinedId를 명확하게 bookId로 받아주는게 좋고
    //밑에는 그냥 joinedId를 리뷰 전용 List로 뽑아준는게 좋을 듯 한디


    //리뷰 한개에 달린 여러장의 이미지
    List<BookFile> findAllByJoinedIdAndFileType(Long joinedId, FileType fileType);

    List<BookFile> findAllByFileTypeAndJoinedIdIn(FileType fileType, List<Long> joinedIds);



}
