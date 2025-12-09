package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import com.nhnacademy.book.repository.BookFileRepository;
import com.nhnacademy.book.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileServiceImpl implements FileService {
    private final BookFileRepository bookFileRepository;

    @Override
    public void saveBookImage(Long bookId, String fileUrl) {
        // 이미 등록된 이미지가 있는지 확인
        Optional<BookFile> existingFile = bookFileRepository.findFirstByFileTypeAndJoinedId(FileType.BOOK, bookId);

        // 있다면 삭제 (덮어쓰기 로직)
        existingFile.ifPresent(bookFileRepository::delete);

        // 새 이미지 생성 및 저장
        BookFile newBookFile = BookFile.builder()
                .fileUrl(fileUrl)
                .fileType(FileType.BOOK) // 카테고리: 도서
                .joinedId(bookId)                // 연결 ID: 도서 ID
                .build();

        bookFileRepository.save(newBookFile);
    }



    @Override
    public String getBookImage(Long bookId) {
        return bookFileRepository.findFirstByFileTypeAndJoinedId(FileType.BOOK, bookId)
                .map(BookFile::getFileUrl)
                .orElse(null);
    }
}
