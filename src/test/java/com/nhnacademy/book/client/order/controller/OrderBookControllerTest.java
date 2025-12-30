package com.nhnacademy.book.client.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book.client.order.dto.OrderBookResponse;
import com.nhnacademy.book.client.order.dto.OrderBookStockRequest;
import com.nhnacademy.book.client.order.service.OrderBookService;
import com.nhnacademy.book.exception.StockNotEnoughException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderBookController.class)
class OrderBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderBookService orderBookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("도서 정보 조회 성공")
    void getAllBooks_Success() throws Exception {
        Long bookId = 1L;
        OrderBookResponse response = new OrderBookResponse(bookId, "Test Book", 1000, true, "image_url");
        
        when(orderBookService.getAllBookByBookIds(List.of(bookId)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/books/info")
                        .param("bookIds", String.valueOf(bookId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(response))));
    }

    @Test
    @DisplayName("재고 감소 성공")
    void decreaseStocks_Success() throws Exception {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 2);
        OrderBookStockRequest request = new OrderBookStockRequest(quantityMap);

        mockMvc.perform(patch("/books/stocks/decrease")
                        .header("X-Saga-Id", sagaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("재고 감소 실패 - 재고 부족")
    void decreaseStocks_Fail_StockNotEnough() throws Exception {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 100);
        OrderBookStockRequest request = new OrderBookStockRequest(quantityMap);

        doThrow(new StockNotEnoughException("재고 부족"))
                .when(orderBookService).decreaseStock(any(UUID.class), any(Map.class));

        mockMvc.perform(patch("/books/stocks/decrease")
                        .header("X-Saga-Id", sagaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 증가 성공")
    void increaseStocks_Success() throws Exception {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 2);
        OrderBookStockRequest request = new OrderBookStockRequest(quantityMap);

        mockMvc.perform(patch("/books/stocks/increase")
                        .header("X-Saga-Id", sagaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("재고 증가 실패 - 잘못된 인자")
    void increaseStocks_Fail_IllegalArgument() throws Exception {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Collections.emptyMap(); // Assuming empty map is handled or mocked to fail
        OrderBookStockRequest request = new OrderBookStockRequest(quantityMap);

        doThrow(new IllegalArgumentException("Invalid Argument"))
                .when(orderBookService).increaseStock(any(UUID.class), any(Map.class));

        mockMvc.perform(patch("/books/stocks/increase")
                        .header("X-Saga-Id", sagaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 롤백 성공")
    void rollbackStocks_Success() throws Exception {
        UUID sagaId = UUID.randomUUID();
        Map<Long, Integer> quantityMap = Map.of(1L, 2);
        OrderBookStockRequest request = new OrderBookStockRequest(quantityMap);

        mockMvc.perform(patch("/books/stocks/rollback")
                        .header("X-Saga-Id", sagaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
