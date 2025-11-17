package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.repository.BookRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
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

    @Override
    public void run(String... args) throws Exception {
        // 데이터 중복 체크
        if(bookRepository.count() > 0) {
            log.info("[CSV] 도서 데이터가 이미 존재합니다. ({} 건). 파싱을 건너뜁니다,", bookRepository.count());
            return;
        }
        log.info("[CSV] 도서 데이터베이스가 비어있습니다. CSV 파싱을 시작합니다...");

        // 파일 경로는 "data/book.csv" 또는 "book.csv" (resources 폴더 기준)
        String filePath = "book.csv"; // (resources/book.csv를 가리킴)

        ClassPathResource resource = new ClassPathResource(filePath);

        // FileReader -> InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {

            // reader로 읽어온 csv 파일을 book 객체로 변환할 변환기 생성
            CsvToBean<Book> csvToBean = new CsvToBeanBuilder<Book>(reader)
                    .withType(Book.class)   // 매핑할 엔티티 클래스
                    .withSeparator(',')     // 구분자
//                    .withSkipLines(1)       // 첫 줄(헤더) 건너뛰기
                    .withIgnoreLeadingWhiteSpace(true) // 공백 무시
                    .withIgnoreEmptyLine(true)
                    .build();

            // CSV 파싱
            List<Book> parsedBooks = csvToBean.parse();
            log.info("[CSV] 파싱 완료: {} 건", parsedBooks.size());

            // NOT NULL 필드 디폴트 값 설정
            for (Book book : parsedBooks) {

                // -- CSV 파일에 없는 값 --
                book.setBookContents("");
                book.setBookPackaging(false);
                book.setBookState(BookState.ON_SALE);
                book.setBookStock(100);
                book.setBookReviewRate("0.0");

                // -- CSV 파일에 있지만 비어있을 수도 있는 필드 --

                // 할인이 안들어가면 기본 값은 판매가 = 정가(정가 0이하면 0)
                int price = book.getBookRegularPrice();
                book.setBookSalePrice(Math.max(price, 0));

                // 필수 값이 비어있는 경우 NOT NULL 위반 방지
                if (book.getBookDescription() == null || book.getBookDescription().isEmpty()) {
                    book.setBookDescription("상세 설명 없음");
                }
                if (book.getBookPublisher() == null || book.getBookPublisher().isEmpty()) {
                    book.setBookPublisher("출판사 정보 없음");
                }
                if (book.getBookImage() == null || book.getBookImage().isEmpty()) {
                    book.setBookImage("/images/default_book.png");
                }
            }
            log.info("[CSV] NOT NULL 필드 디폴트 값 설정 완료");

            log.info("[CSV] DB에 Batch Insert 시작... ({}개 씩)", 1000);
            bookRepository.saveAll(parsedBooks);
            log.info("[CSV] 총 {} 건의 도서 데이터 저장 완료.", parsedBooks.size());

        } catch (Exception e) {
            log.error("[CSV] 파일 파싱 또는 DB 저장 중 심각한 오류 발생", e);
            // 여기서 예외를 던지면 애플리케이션 시작이 중단됩니다.
            throw new RuntimeException("CSV 데이터 로딩 실패!", e);
        }
    }
}