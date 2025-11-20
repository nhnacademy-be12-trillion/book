package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.Author;
import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.BookState;
import com.nhnacademy.book.entity.Publisher;
import com.nhnacademy.book.repository.AuthorRepository;
import com.nhnacademy.book.repository.BookRepository;
import com.nhnacademy.book.repository.PublisherRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class BookParser {
    private final CustomDateConverter customDateConverter;

    private final BookRepository bookRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;

    public void parse(Reader reader) throws IOException, CsvException {
        List<String[]> allLines;
        List<Book> books = new ArrayList<>();
        Set<Publisher> publishers = new HashSet<>();
        Set<Author> authors = new HashSet<>();

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

                        Publisher publisher = new Publisher(publisherNm);
                        Author author = new Author(authrNm);

                        if (!publisher.getPublisherName().isBlank()) {
                            publishers.add(publisher);
                        }

                        if (!author.getAuthorName().isBlank()) {
                            authors.add(author);
                        }

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
                                imageUrl.isEmpty() ? "/images/default_book.png" : imageUrl
                            );

                            books.add(book);
                        } catch (CsvDataTypeMismatchException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        publisherRepository.saveAll(publishers);
        authorRepository.saveAll(authors);

        log.info("[CSV] 모든 데이터 읽기 완료: {} 건", allLines.size());

        log.info("[CSV] DB에 Batch Insert 시작... ({}개 씩)", 1000);
        bookRepository.saveAll(books);
        log.info("[CSV] 총 {} 건의 도서 데이터 저장 완료.", books.size());
    }
}