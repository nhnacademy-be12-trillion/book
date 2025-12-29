package com.nhnacademy.book.point;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "member-service")
public interface PointClient {
    @PostMapping("/members/points/review")
    void awardReviewPoints(@RequestHeader("X-Member-Id") Long memberId,
                           @RequestBody ReviewPointRequest request);
}
