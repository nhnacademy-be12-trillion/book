package com.nhnacademy.book.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 연결 타임아웃은 5초면 충분합니다.
        factory.setConnectTimeout(5000);

        // [수정] AI 응답 대기 시간을 위해 읽기 타임아웃을 30초(30000ms)로 늘립니다.
        factory.setReadTimeout(30000);

        return new RestTemplate(factory);
    }
}