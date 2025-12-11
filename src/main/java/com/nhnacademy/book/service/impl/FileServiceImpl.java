package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import com.nhnacademy.book.repository.BookFileRepository;
import com.nhnacademy.book.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.LifecycleState;
import org.bouncycastle.util.Pack;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    private final BookFileRepository bookFileRepository;

    @Override
    @Transactional
    public void saveBookImage(Long bookId, String fileUrl) {
        // 이미 등록된 이미지가 있는지 확인
        Optional<BookFile> existingFile = bookFileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK);

        // 있다면 삭제 (덮어쓰기 로직)
        existingFile.ifPresent(bookFileRepository::delete);

        // 새 이미지 생성 및 저장
        BookFile newBookFile = BookFile.builder()
                .fileUrl(fileUrl)
                .fileType(FileType.BOOK) // 카테고리: 도서
                .joinedId(bookId)        // 연결 ID: 도서 ID
                .build();

        bookFileRepository.save(newBookFile);
    }

    //리뷰 등록할때 파일 등록 -> 리뷰에는 여러 이미지가 달릴 수 있음.
    @Override
    @Transactional
    public void saveReviewImages(Long reviewId, List<String> fileUrl){
        if(fileUrl == null || fileUrl.isEmpty()){
            return;
        }
        List<BookFile> reviewFiles = fileUrl.stream()
                .map(url -> BookFile.builder()
                        .joinedId(reviewId)        // 리뷰 ID 연결
                        .fileType(FileType.REVIEW) // 타입: 리뷰
                        .fileUrl(url)
                        .build())
                .toList();

        bookFileRepository.saveAll(reviewFiles);
    }

    //도서 이미지 가져오기
    @Override
    @Transactional(readOnly = true)
    public String getBookImage(Long bookId) {
        return bookFileRepository.findFirstByJoinedIdAndFileType(bookId, FileType.BOOK)
                .map(BookFile::getFileUrl)
                .orElse(null);
    }

    //리뷰 1개에 달린 이미지 리스트 조회
    @Override
    @Transactional(readOnly = true)
    public List<String> getReviewImages(Long reviewId){
        List<BookFile> files = bookFileRepository.findAllByJoinedIdAndFileType(reviewId,FileType.REVIEW);

        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        return files.stream()
                .map(BookFile::getFileUrl)
                .toList();

    }
}
