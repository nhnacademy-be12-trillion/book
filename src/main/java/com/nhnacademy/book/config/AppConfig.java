package com.nhnacademy.book.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 1. 요청 팩토리 생성
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 2. 타임아웃 설정 (5초) - 밀리초 단위
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        // 3. 팩토리를 넣어서 RestTemplate 생성
        return new RestTemplate(factory);
    }
}