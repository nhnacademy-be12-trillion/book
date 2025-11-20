package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class BookParser {
    private final CustomDateConverter customDateConverter;

    public List<Book> parse(Reader reader) throws IOException, CsvException {
        List<String[]> allLines;
        List<Book> books = new ArrayList<>();

        try (CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
            allLines = csvReader.readAll();

            // 요청하신 대로 forEach 블록에서 각 필드를 변수로 받습니다.
            allLines.stream()
                    .filter(line -> line.length >= 18)
                    .forEach(line -> {
                        String seqNo = line[0].trim();
                        String isbnThirteenNo = line[1].trim();
                        String vlmNm = line[2].trim();
                        String titleNm = line[3].trim();
                        String authrNm = line[4].trim();
                        String publisherNm = line[5].trim();
                        String pblicteDe = line[6].trim();
                        String adtionSmblNm = line[7].trim();
                        String prcValue = line[8].trim();
                        String imageUrl = line[9].trim();
                        String bookIntrcnCn = line[10].trim();
                        String kdcNm = line[11].trim();
                        String titleSbstNm = line[12].trim();
                        String authrSbstNm = line[13].trim();
                        String twoPblicteDe = line[14].trim();
                        String intntBookstBookExstAt = line[15].trim();
                        String portalSiteBookExstAt = line[16].trim();
                        String isbnNo = line[17].trim();

                        try {
                            Book book = new Book(
                                null,
                                isbnThirteenNo,
                                titleNm,
                                bookIntrcnCn,
                                publisherNm,
                                (LocalDate) customDateConverter.convert(twoPblicteDe),
                                "",
                                false,
                                BookState.ON_SALE,
                                100,
                                prcValue.isEmpty() ? 0 : (int) Double.parseDouble(prcValue),
                                0,
                                0,
                                imageUrl.isEmpty() ? "/images/default_book.png" : imageUrl
                            );

                            books.add(book);
                        } catch (CsvDataTypeMismatchException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        log.info("[CSV] 모든 데이터 읽기 완료: {} 건", allLines.size());
        return books;
    }
}