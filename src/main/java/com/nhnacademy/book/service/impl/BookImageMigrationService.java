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

    // 동시에 처리할 스레드 개수 (너무 높으면 알라딘에서 차단당할 수 있음. 10~20 추천)
    private static final int THREAD_POOL_SIZE = 20;

    @Async
    public void migrateAllImages() {
        // 1. 전체 데이터 조회 (메모리 부족 시 Pageable 사용 고려)
        List<Book> candidates = bookRepository.findByBookImageIsNotNull();

        // [테스트용 제한 로직이 필요하다면 여기에 유지, 실제는 전체 수행]
        // if (candidates.size() > 50) candidates = candidates.subList(0, 50);

        log.info("🚀 고속 이미지 이관 시작! 대상: {}권, 동시 처리 수: {}", candidates.size(), THREAD_POOL_SIZE);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 2. 고정된 스레드 풀 생성 (20개 스레드가 동시에 일함)
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            // 3. 비동기 작업 생성 및 실행
            List<CompletableFuture<Void>> futures = candidates.stream()
                    .map(book -> CompletableFuture.runAsync(() -> {
                        try {
                            processSingleBook(book); // 개별 트랜잭션 실행
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            log.error("❌ 실패 (ID: {}): {}", book.getBookId(), e.getMessage());
                        }
                    }, executor)) // 지정한 스레드 풀 사용
                    .toList();

            // 4. 모든 작업이 끝날 때까지 대기 (Blocking)
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } finally {
            executor.shutdown(); // 스레드 풀 정리
        }

        log.info("✅ 이관 종료! 성공: {}, 실패: {}", successCount.get(), failCount.get());
    }

    /**
     * 개별 책 처리 (기존 로직 유지)
     * 트랜잭션을 REQUIRES_NEW로 유지해야 각 스레드에서 별도 커밋이 발생함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processSingleBook(Book book) {
        String originalUrl = book.getBookImage();

        if (originalUrl == null || originalUrl.isBlank()) return;

        // MinIO 업로드 (네트워크 I/O 발생 구간)
        String newMinioUrl = minioService.uploadFromUrl(originalUrl);

        if (newMinioUrl != null) {
            BookFile bookFile = fileRepository.findFirstByFileTypeAndJoinedId(FileType.BOOK, book.getBookId())
                    .orElse(BookFile.builder()
                            .fileType(FileType.BOOK)
                            .joinedId(book.getBookId())
                            .build());

            bookFile.setFileUrl(newMinioUrl);
            fileRepository.save(bookFile); // DB 저장

            book.setBookImage(null); // 원본 컬럼 비우기
        } else {
            throw new RuntimeException("이미지 다운로드/업로드 실패");
        }
    }
}