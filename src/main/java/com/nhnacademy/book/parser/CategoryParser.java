package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.Category;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class CategoryParser {

    public List<Category> parse(Reader reader) {
        Map<Long, Category> categoryMapByCode = new HashMap<>();

        try (CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
            List<String[]> allLines = csvReader.readAll();

            // 카테고리 객체 생성
            allLines.stream()
                    .filter(line -> !(line.length < 2) && !(line[0].trim().isEmpty()))
                    .forEach(line -> {
                        long id = Long.parseLong(line[0].trim());
                        String name = line[1].trim();

                        Category category = new Category(id, name);

                        categoryMapByCode.put(id, category);
                    });

            // 카테고리 연관 관계 매핑
            categoryMapByCode.values()
                    .forEach(category -> {
                        Long parentCode = getParentCode(category.getCategoryId());
                        if (parentCode != null) {
                            Category parent = categoryMapByCode.get(parentCode);
                            category.setParent(parent);
                        }
                    });
        }  catch (Exception e) {
            log.error("카테고리 파싱 실패: {}", e.getMessage(), e);
        }

        return new ArrayList<>(categoryMapByCode.values());
    }

    private Long getParentCode(Long code) {
        String codeStr = code.toString();

        if (codeStr.length() == 3) {
            // X00 -> 최상위 카테고리
            if (codeStr.endsWith("00")) {
                return null;
            }

            // XY0 -> X00이 부모 카테고리
            if (codeStr.endsWith("0")) {
                return (code / 100) * 100;
            }
        }

        // 00X, 0XY -> 000, 0X0이 부모 카테고리
        return (code / 10) * 10;
    }
}