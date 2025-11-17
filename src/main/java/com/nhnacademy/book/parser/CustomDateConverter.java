package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.Book;
import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class CustomDateConverter extends AbstractBeanField<Book, LocalDate> {

    // 2가지 형식의 포맷터를 미리 정의
    private static final DateTimeFormatter FORMATTER_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATTER_NODASH = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    protected Object convert(String value) throws CsvDataTypeMismatchException {
        if (value == null || value.isEmpty()) {
            return LocalDate.now(); // CSV 값이 비어있으면 현재 날짜
        }

        try {
            if (value.contains("-")) {
                // "yyyy-MM-dd" 형식 파싱
                return LocalDate.parse(value, FORMATTER_DASH);
            } else if (value.length() == 8) {
                // "yyyyMMdd" 형식 파싱
                return LocalDate.parse(value, FORMATTER_NODASH);
            } else {
                // 알 수 없는 형식
                log.warn("[CSV] 알 수 없는 날짜 형식 ({}): 기본값(오늘 날짜) 사용", value);
                return LocalDate.now();
            }
        } catch (DateTimeParseException e) {
            // "19982001" (20월) 또는 "19940800" (0일) 같은 잘못된 날짜 파싱 실패 시
            // 예외를 던지지 않고, 로그만 남기고 기본값(오늘 날짜)을 반환
            log.warn("[CSV] 잘못된 날짜 데이터 ({}): 기본값(오늘 날짜) 사용", value);
            return LocalDate.now();
        }
    }
}