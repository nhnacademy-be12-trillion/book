package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.Book;
import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomPriceConverter extends AbstractBeanField<Book, Integer> { // Integer 반환

    @Override
    protected Object convert(String value) throws CsvDataTypeMismatchException {
        if (value == null || value.isEmpty()) {
            return 0; // CSV 값이 비어있으면 0
        }

        // value.trim()을 사용하여 앞뒤 공백을 모두 제거
        String trimmedValue = value.trim();

        if (trimmedValue.isEmpty()) {
            return 0; // 공백만 있는 경우 0
        }

        try {
            if (trimmedValue.contains(".")) {
                // "10590.00" 같은 소수점 문자열 처리
                Double doubleValue = Double.parseDouble(trimmedValue);
                return doubleValue.intValue(); // Double -> int
            } else {
                // "16000" 같은 정수 문자열 처리
                return Integer.parseInt(trimmedValue);
            }
        } catch (NumberFormatException e) {
            log.warn("[CSV] 가격 형식 파싱 실패 ({}): 기본값(0) 사용", value);
            // 예외를 던지는 대신 0을 반환하여 파싱을 계속 진행
            return 0;
        }
    }
}