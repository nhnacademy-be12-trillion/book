package com.nhnacademy.book.client.order.controller;

import com.nhnacademy.book.client.order.dto.OrderBook;
import com.nhnacademy.book.client.order.dto.OrderBookResponse;
import com.nhnacademy.book.client.order.dto.OrderBookStockRequest;
import com.nhnacademy.book.client.order.service.OrderBookService;
import com.nhnacademy.book.exception.StockNotEnoughException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class OrderBookController {
    private final OrderBookService orderBookService;

    @GetMapping("/books/info")
    public ResponseEntity<List<OrderBookResponse>> getAllBooks(@RequestParam List<Long> bookIds) {
        List<OrderBookResponse> responses = orderBookService.getAllBookByBookIds(bookIds);

        return ResponseEntity.ok(responses);
    }

    // URI가 RESTful하지 않지만 의도가 명확해서 내부 통신용으로는 괜찮을듯 함
    @PatchMapping("/books/stocks/decrease")
    public ResponseEntity<String> decreaseStocks(@RequestHeader("X-SAGA-ID") String sagaHeader,
                                               @RequestBody OrderBookStockRequest request) {
        try {
            UUID sagaId = UUID.fromString(sagaHeader);

            orderBookService.decreaseStock(sagaId, request.quantityMap());
        } catch (NullPointerException | IllegalArgumentException | StockNotEnoughException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/books/stocks/increase")
    public ResponseEntity<String> increaseStocks(@RequestHeader("X-SAGA-ID") String sagaHeader,
                                               @RequestBody OrderBookStockRequest request) {
        try {
            UUID sagaId = UUID.fromString(sagaHeader);

            orderBookService.increaseStock(sagaId, request.quantityMap());
        } catch (NullPointerException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/books/stock/rollback")
    public ResponseEntity<String> rollbackStocks(@RequestHeader("X-SAGA-ID") String sagaHeader,
                                               @RequestBody OrderBookStockRequest request) {
        try {
            UUID sagaId = UUID.fromString(sagaHeader);

            orderBookService.rollbackStock(sagaId, request.quantityMap());
        } catch (NullPointerException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
