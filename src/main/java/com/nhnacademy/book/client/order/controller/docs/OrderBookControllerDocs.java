package com.nhnacademy.book.client.order.controller.docs;

import com.nhnacademy.book.client.order.dto.OrderBookResponse;
import com.nhnacademy.book.client.order.dto.OrderBookStockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Order Book", description = "주문 연동 도서 API")
public interface OrderBookControllerDocs {

    @Operation(summary = "주문 도서 정보 조회", description = "주문 처리를 위해 도서 정보를 조회합니다.")
    ResponseEntity<List<OrderBookResponse>> getAllBooks(
            @Parameter(description = "도서 ID 리스트", required = true) List<Long> bookIds
    );

    @Operation(summary = "재고 차감", description = "주문 시 도서 재고를 차감합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재고 차감 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 재고 부족", content = @Content(schema = @Schema(implementation = String.class)))
    })
    ResponseEntity<String> decreaseStocks(
            @Parameter(description = "Saga ID", required = true) UUID sagaId,
            @Parameter(description = "재고 변경 요청", required = true) OrderBookStockRequest request
    );

    @Operation(summary = "재고 증가", description = "주문 취소 시 도서 재고를 증가시킵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재고 증가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = String.class)))
    })
    ResponseEntity<String> increaseStocks(
            @Parameter(description = "Saga ID", required = true) UUID sagaId,
            @Parameter(description = "재고 변경 요청", required = true) OrderBookStockRequest request
    );

    @Operation(summary = "재고 롤백", description = "트랜잭션 실패 시 재고를 롤백합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재고 롤백 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = String.class)))
    })
    ResponseEntity<String> rollbackStocks(
            @Parameter(description = "Saga ID", required = true) UUID sagaId,
            @Parameter(description = "재고 변경 요청", required = true) OrderBookStockRequest request
    );
}
