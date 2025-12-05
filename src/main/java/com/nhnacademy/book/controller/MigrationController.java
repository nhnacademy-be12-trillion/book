package com.nhnacademy.book.controller;

import com.nhnacademy.book.service.impl.BookImageMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final BookImageMigrationService migrationService;

    /**
     * 관리자용: 도서 이미지 이관 작업 시작
     * 호출 시 백그라운드에서 작업이 시작되며, 브라우저에는 바로 "시작됨" 메시지 반환
     */
    @PostMapping("/start")
    public ResponseEntity<String> startMigration() {
        migrationService.migrateAllImages();
        return ResponseEntity.ok("🚀 이미지 이관 작업이 시작되었습니다. (Book -> File 테이블 이동)");
    }
}