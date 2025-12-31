package com.nhnacademy.book.controller;

import com.nhnacademy.book.controller.docs.AdminBookControllerDocs;
import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import com.nhnacademy.book.service.BookService;
import com.nhnacademy.book.service.impl.BookAiRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/books")
public class AdminBookController implements AdminBookControllerDocs {
    private final BookService bookService;
    private final BookAiRegistrationService bookAiRegistrationService;

    // 도서 등록 API
    // POST /api/admin/books
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Long> createBook(
            @RequestPart("book") BookCreateRequest request,
            // "image" -> "file" 로 변경
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        // 변수명도 image -> file로 맞춤 (Service 메서드 파라미터 이름과 통일)
        Long bookId = bookService.createBook(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookId);
    }


    // 도서 수정 API
    // PUT /api/admin/books/{book-id}
    @PutMapping("/{book-id}")
    public ResponseEntity<Void> updateBook(@PathVariable("book-id") Long bookId, @RequestBody BookUpdateRequest request) {
        bookService.updateBook(bookId, request);
        return ResponseEntity.noContent().build(); // 반환할 내용 없음 204
    }

    // 도서 삭제 API
    // DELETE /api/admin/books/{book-id}
    @DeleteMapping("/{book-id}")
    public ResponseEntity<Void> deleteBook(@PathVariable("book-id") Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();  // 반환할 내용 없음 204
    }

    // 알라딘 API에서 ISBN으로 도서 정보 호출 API
    // GET /api/admin/books/isbn/{isbn}
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookCreateRequest> getBookInfoByIsbn(@PathVariable String isbn) {
        BookCreateRequest response = bookAiRegistrationService.getBookInfoByIsbn(isbn);
        return ResponseEntity.ok(response);
    }

}
