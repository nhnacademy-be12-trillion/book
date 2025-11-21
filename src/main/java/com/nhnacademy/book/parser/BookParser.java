package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.repository.*;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class BookParser {
    private final CustomDateConverter customDateConverter;
    private final CategoryRepository categoryRepository;

    public List<Book> parse(Reader reader) throws IOException, CsvException {
        List<String[]> allLines;

        List<Book> books = new ArrayList<>();

        Map<Long, Category> categoryMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getCategoryId, Function.identity()));

        try (CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
            allLines = csvReader.readAll();

            // 일단 모든 필드 파싱
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

                    // 도서
                    try {
                        Book book = new Book(
                            null,
                            isbnThirteenNo,
                            titleNm,
                            bookIntrcnCn,
                            (LocalDate) customDateConverter.convert(twoPblicteDe),
                            "",
                            false,
                            BookState.ON_SALE,
                            100,
                            prcValue.isEmpty() ? 0 : (int) Double.parseDouble(prcValue),
                            0,
                            0,
                            imageUrl.isEmpty() ? "/images/default_book.png" : imageUrl,
                            new HashSet<>(),
                            new HashSet<>(),
                            null
                        );

                        // 출판사
                        Publisher publisher = new Publisher(publisherNm);
                        if (!publisher.getPublisherName().isBlank()) {
                            book.setPublisher(publisher);
                        }

                        if (!authrNm.isBlank()) {
                            String[] authors = authrNm.split(",");

                            // 도서-작가
                            Arrays.stream(authors)
                                    .map(String::trim)
                                    .forEach(authorStr -> {
                                        Author author = new Author(authorStr);
                                        BookAuthor bookAuthor = new BookAuthor(author, book);

                                        book.getBookAuthors().add(bookAuthor);
                                    });
                        }

                        // 도서-카테고리
                        if (!kdcNm.isBlank()) {
                            long categoryId = (long) Double.parseDouble(kdcNm);
                            Category category = categoryMap.get(categoryId);
                            if (category != null) {
                                BookCategory bookCategory = new BookCategory(category, book);
                                book.getBookCategories().add(bookCategory);
                            }
                        }

                        books.add(book);
                    } catch (CsvDataTypeMismatchException e) {
                        throw new RuntimeException(e);
                    }
                });
        }

        return books;
    }
}