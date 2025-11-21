package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Category;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsvParser implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryParser categoryParser;
    private final BookParser bookParser;

    @Override
    public void run(String... args) {

        // 카테고리 파싱 시작
        if (categoryRepository.count() > 0) {
            log.info("[CSV] 카테고리 데이터가 이미 존재합니다. ({} 건). 파싱을 건너뜁니다,", categoryRepository.count());
        } else {
            log.info("[CSV] 카테고리 데이터베이스가 비어있습니다. CSV 파싱을 시작합니다...");

            try {
                String categoryFilePath = "category.csv";

                ClassPathResource resource = new ClassPathResource(categoryFilePath);

                Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);

                List<Category> categories = categoryParser.parse(reader);

                categoryRepository.saveAll(categories);
            } catch (Exception e) {
                log.info("[CSV] 카테고리 데이터 파싱 실패: {}", e.getMessage(), e);
            }
        }

        // 도서 파싱 시작
        if (bookRepository.count() > 0) {
            log.info("[CSV] 도서 데이터가 이미 존재합니다. ({} 건). 파싱을 건너뜁니다,", bookRepository.count());
        } else {
            log.info("[CSV] 도서 데이터베이스가 비어있습니다. CSV 파싱을 시작합니다...");

            try {
                // 파일 경로는 "data/book.csv" 또는 "book.csv" (resources 폴더 기준)
                String filePath = "book.csv"; // (resources/book.csv를 가리킴)

                ClassPathResource resource = new ClassPathResource(filePath);

                Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);

                List<Book> books = bookParser.parse(reader);

                bookRepository.saveAll(books);

                log.info("[CSV] 총 {} 건의 도서 데이터 저장 완료.", books.size());
            } catch (Exception e) {
                log.info("[CSV] 도서 데이터 파싱 실패: {}", e.getMessage(), e);
            }
        }
    }
}