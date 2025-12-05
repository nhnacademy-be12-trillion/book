package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<BookFile, Long> {
    //도서 1개의 이미지
    Optional<BookFile> findFirstByFileTypeAndJoinedId(FileType fileType, Long fileId);
}
