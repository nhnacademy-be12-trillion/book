package com.nhnacademy.book.client.order.controller;

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

    @PatchMapping("/books/stocks/decrease")
    public ResponseEntity<String> decreaseStocks(@RequestHeader("X-Saga-Id") UUID sagaId,
                                               @RequestBody OrderBookStockRequest request) {
        try {
            orderBookService.decreaseStock(sagaId, request.quantityMap());
        } catch (NullPointerException | IllegalArgumentException | StockNotEnoughException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/books/stocks/increase")
    public ResponseEntity<String> increaseStocks(@RequestHeader("X-Saga-Id") UUID sagaId,
                                               @RequestBody OrderBookStockRequest request) {
        try {
            orderBookService.increaseStock(sagaId, request.quantityMap());
        } catch (NullPointerException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/books/stocks/rollback")
    public ResponseEntity<String> rollbackStocks(@RequestHeader("X-Saga-Id") UUID sagaId,
                                               @RequestBody OrderBookStockRequest request) {
        try {
            orderBookService.rollbackStock(sagaId, request.quantityMap());
        } catch (NullPointerException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
