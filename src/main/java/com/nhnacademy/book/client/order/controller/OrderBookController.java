package com.nhnacademy.book.client.order.controller;

import com.nhnacademy.book.client.order.dto.OrderBookResponse;
import com.nhnacademy.book.client.order.dto.OrderBookStockRequest;
import com.nhnacademy.book.client.order.service.OrderBookService;
import com.nhnacademy.book.exception.NotEnoughStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class OrderBookController {
    private final OrderBookService orderBookService;

    @GetMapping("/api/order-books")
    public ResponseEntity<List<OrderBookResponse>> getAllBooks(@RequestParam List<Long> bookIds) {
        List<OrderBookResponse> responses = orderBookService.getAllBookByBookIds(bookIds);

        return ResponseEntity.ok(responses);
    }

    // URI가 RESTful하지 않지만 의도가 명확해서 괜찮을듯 함
    @PatchMapping("/api/order-books/decrease-stocks")
    public ResponseEntity<Void> decreaseStocks(@RequestHeader("X-SAGA-ID") String sagaHeader,
                                               @RequestBody OrderBookStockRequest request) {
        UUID sagaId = UUID.fromString(sagaHeader);

        try {
            orderBookService.decreaseStock(sagaId, request.quantityMap());
        } catch (NotEnoughStockException e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/api/order-books/increase-stocks")
    public ResponseEntity<Void> increaseStocks(@RequestHeader("X-SAGA-ID") String sagaHeader,
                                               @RequestBody OrderBookStockRequest request) {
        UUID sagaId = UUID.fromString(sagaHeader);

        orderBookService.increaseStock(sagaId, request.quantityMap());

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/api/order-books/rollback-stocks")
    public ResponseEntity<Void> rollbackStocks(@RequestHeader("X-SAGA-ID") String sagaHeader,
                                               @RequestBody OrderBookStockRequest request) {
        UUID sagaId = UUID.fromString(sagaHeader);

        orderBookService.rollbackStock(sagaId, request.quantityMap());

        return ResponseEntity.ok().build();
    }
}
