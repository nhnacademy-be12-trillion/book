package com.nhnacademy.book.controller.docs;

import com.nhnacademy.book.dto.book.BookCreateRequest;
import com.nhnacademy.book.dto.book.BookUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Book", description = "관리자 도서 관리 API")
public interface AdminBookControllerDocs {

    @Operation(summary = "도서 등록", description = "새로운 도서를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "도서 등록 성공", content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ResponseEntity<Long> createBook(
            @Parameter(description = "도서 생성 정보", required = true) BookCreateRequest request,
            @Parameter(description = "도서 이미지 파일") MultipartFile file
    );

    @Operation(summary = "도서 수정", description = "기존 도서 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "도서 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음")
    })
    ResponseEntity<Void> updateBook(
            @Parameter(description = "도서 ID", required = true) Long bookId,
            @Parameter(description = "도서 수정 정보", required = true) BookUpdateRequest request
    );

    @Operation(summary = "도서 삭제", description = "도서를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "도서 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음")
    })
    ResponseEntity<Void> deleteBook(
            @Parameter(description = "도서 ID", required = true) Long bookId
    );

    @Operation(summary = "ISBN으로 도서 정보 조회 (알라딘 API)", description = "ISBN을 이용하여 외부 API(알라딘)에서 도서 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "도서 정보 조회 성공", content = @Content(schema = @Schema(implementation = BookCreateRequest.class))),
            @ApiResponse(responseCode = "404", description = "도서 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "502", description = "외부 API 호출 실패")
    })
    ResponseEntity<BookCreateRequest> getBookInfoByIsbn(
            @Parameter(description = "ISBN", required = true) String isbn
    );
}
