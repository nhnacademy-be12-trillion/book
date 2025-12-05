package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import com.nhnacademy.book.repository.FileRepository;
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
    private final FileRepository fileRepository;

    @Override
    public void saveBookImage(Long bookId, String fileUrl) {
        // 1. 혹시 이미 등록된 이미지가 있는지 확인
        Optional<BookFile> existingFile = fileRepository.findFirstByFileTypeAndJoinedId(FileType.BOOK, bookId);

        // 2. 있다면 삭제 (덮어쓰기 로직)
        // (Entity에 update 메서드가 없으므로, 지우고 다시 만드는 게 깔끔합니다)
        existingFile.ifPresent(fileRepository::delete);

        // 3. 새 이미지 생성 및 저장
        BookFile newBookFile = BookFile.builder()
                .fileUrl(fileUrl)
                .fileType(FileType.BOOK) // 카테고리: 도서
                .joinedId(bookId)                // 연결 ID: 도서 ID
                .build();

        fileRepository.save(newBookFile);
    }



    @Override
    public String getBookImage(Long bookId) {
        return fileRepository.findFirstByFileTypeAndJoinedId(FileType.BOOK, bookId)
                .map(BookFile::getFileUrl)
                .orElse(null);
    }
}
