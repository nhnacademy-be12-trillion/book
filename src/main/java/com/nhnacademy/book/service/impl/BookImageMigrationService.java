package com.nhnacademy.book.service.impl;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookFile;
import com.nhnacademy.book.entity.FileType;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookImageMigrationService {

    private final BookRepository bookRepository;
    private final FileRepository fileRepository;
    private final MinioService minioService;

    // 동시에 처리할 스레드 개수 (너무 높으면 알라딘 차단 위험)
    private static final int THREAD_POOL_SIZE = 20;

    /**
     * [기능 1] 이미지 이관 (Book -> File 테이블)
     * 비동기 + 멀티스레드로 빠르게 처리합니다.
     */
    @Async
    public void migrateAllImages() {
        List<Book> candidates = bookRepository.findByBookImageIsNotNull();

        log.info("🚀 고속 이미지 이관 시작! 대상: {}권, 동시 처리 수: {}", candidates.size(), THREAD_POOL_SIZE);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            List<CompletableFuture<Void>> futures = candidates.stream()
                    .map(book -> CompletableFuture.runAsync(() -> {
                        try {
                            processSingleBook(book);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            log.error("❌ 실패 (ID: {}): {}", book.getBookId(), e.getMessage());
                        }
                    }, executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } finally {
            executor.shutdown();
        }

        log.info("✅ 이미지 이관 종료! 성공: {}, 실패: {}", successCount.get(), failCount.get());
    }

    /**
     * [기능 2] 가격 데이터 보정 (추가됨)
     * 정가 0원 -> 1만원, 판매가 0원 -> 정가로 수정
     */
    @Transactional
    public String fixBookPrices() {
        log.info("💰 가격 데이터 보정 작업 시작...");

        // 정가 0원 -> 10,000원
        int updatedRegular = bookRepository.updateZeroRegularPricesToDefault();
        log.info("👉 정가 보정 완료: {}건", updatedRegular);

        // 판매가 0원 -> 정가와 동일하게
        int updatedSale = bookRepository.updateZeroSalePricesToRegularPrice();
        log.info("👉 판매가 보정 완료: {}건", updatedSale);

        return String.format("가격 수정 완료 (정가: %d건, 판매가: %d건)", updatedRegular, updatedSale);
    }

    // (내부 메서드) 이미지 개별 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processSingleBook(Book book) {
        String originalUrl = book.getBookImage();
        if (originalUrl == null || originalUrl.isBlank()) return;

        String newMinioUrl = minioService.uploadFromUrl(originalUrl);

        if (newMinioUrl != null) {
            BookFile bookFile = fileRepository.findFirstByFileTypeAndJoinedId(FileType.BOOK, book.getBookId())
                    .orElse(BookFile.builder()
                            .fileType(FileType.BOOK)
                            .joinedId(book.getBookId())
                            .build());

            bookFile.setFileUrl(newMinioUrl);
            fileRepository.save(bookFile);

            book.setBookImage(null);
        } else {
            throw new RuntimeException("이미지 다운로드/업로드 실패");
        }
    }
}