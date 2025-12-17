package com.nhnacademy.book.client.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/order-items/top-selling")
    List<Long> getTopSellingBookIds(@RequestParam int limit);
}
