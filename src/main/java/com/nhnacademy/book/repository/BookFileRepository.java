package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookFileRepository extends JpaRepository<BookFile, Long> {

    //도서 1개의 이미지
    Optional<BookFile> findFirstByJoinedIdAndFileType(Long joinedId, FileType fileType);

    //리뷰 한개에 달린 여러장의 이미지
    List<BookFile> findAllByJoinedIdAndFileType(Long joinedId, FileType fileType);

    // 여러 연결 ID에 해당하는 특정 타입의 파일 목록 조회
    List<BookFile> findAllByJoinedIdInAndFileType(List<Long> joinedIds, FileType fileType);

}
