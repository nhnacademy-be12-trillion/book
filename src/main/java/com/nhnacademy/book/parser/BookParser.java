package com.nhnacademy.book.parser;

import com.nhnacademy.book.entity.*;
import com.nhnacademy.book.repository.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
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
    private final TagRepository tagRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;

    public List<Book> parse(Reader reader) throws IOException, CsvException {
        List<String[]> allLines;
        List<Book> books = new ArrayList<>();

        // 카테고리 Map 로딩
        Map<Long, Category> categoryMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getCategoryId, Function.identity()));

        // 태그 Map 로딩
        Map<String, Tag> tagMap = tagRepository.findAll().stream()
                .collect(Collectors.toMap(Tag::getTagName, Function.identity()));

        // DB에 이미 저장된 출판사 로딩 (중복 방지용)
        // (key: 이름, value: 객체)
        Map<String, Publisher> publisherCache = publisherRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Publisher::getPublisherName,
                        Function.identity(),
                        (p1, p2) -> p1 // 혹시 DB에 중복된 이름이 있다면 첫 번째 것 사용
                ));

        // DB에 이미 저장된 작가 로딩 (중복 방지용)
        Map<String, Author> authorCache = authorRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Author::getAuthorName,
                        Function.identity(),
                        (a1, a2) -> a1
                ));

        try (CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
            allLines = csvReader.readAll();

            allLines.stream()
                    .filter(line -> line.length >= 18) // 유효한 컬럼 개수 확인
                    .forEach(line -> {
                        // CSV 컬럼 파싱
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
                            // Book 객체 생성
                            Book book = new Book(
                                    null, // ID는 자동 생성
                                    isbnThirteenNo,
                                    titleNm,
                                    bookIntrcnCn,
                                    (LocalDate) customDateConverter.convert(twoPblicteDe),
                                    "", // bookIndex (목차)는 빈 문자열로 초기화
                                    true, // 포장은 기본으로 있는걸로 가정.
                                    BookState.ON_SALE,
                                    100, // 재고
                                    prcValue.isEmpty() ? 0 : (int) Double.parseDouble(prcValue), // 정가
                                    prcValue.isEmpty() ? 0 : (int) Double.parseDouble(prcValue), //  판매가 == 초기 정가랑 같음 지정 안해주면
                                    0, // 리뷰 점수
                                    imageUrl.isEmpty() ? "/images/default_book.png" : imageUrl,
                                    new HashSet<>(), // authors
                                    new HashSet<>(), // categories
                                    new HashSet<>(), // tags
                                    null, // publisher
                                    0 // viewCount
                            );

                            // 출판사 연결
                            if (!publisherNm.isBlank()) {
                                // Map(Cache)에서 찾고, 없으면 새로 만들어서 Map에 넣고 반환
                                Publisher publisher = publisherCache.computeIfAbsent(publisherNm, name -> new Publisher(name));
                                book.setPublisher(publisher);
                            }

                            // 작가 연결
                            if (!authrNm.isBlank()) {
                                String[] authors = authrNm.split(","); // 쉼표로 구분된 여러 작가 처리

                                Arrays.stream(authors)
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .forEach(authorStr -> {
                                            // Map(Cache)에서 찾고, 없으면 새로 생성
                                            Author author = authorCache.computeIfAbsent(authorStr, name -> new Author(name));

                                            // 연결 테이블 엔티티 생성
                                            BookAuthor bookAuthor = new BookAuthor(author, book);
                                            book.getBookAuthors().add(bookAuthor);
                                        });
                            }

                            // 카테고리 및 태그 연결
                            if (!kdcNm.isBlank()) {
                                try {
                                    long categoryId = (long) Double.parseDouble(kdcNm);
                                    Category category = categoryMap.get(categoryId);

                                    if (category != null) {
                                        // 도서-카테고리 연결
                                        BookCategory bookCategory = new BookCategory(category, book);
                                        book.getBookCategories().add(bookCategory);

                                        // 카테고리 이름으로 태그 연결
                                        Tag tag = tagMap.get(category.getCategoryName());
                                        if (tag != null) {
                                            BookTag bookTag = new BookTag(tag, book);
                                            book.getBookTags().add(bookTag);
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    // 카테고리 ID가 숫자가 아닌 경우 무시
                                    log.warn("Invalid category ID format: {}", kdcNm);
                                }
                            }

                            // 필수 데이터 유효성 검사 후 리스트에 추가
                            if (book.getBookPublicationDate() != null
                                    && !book.getBookDescription().isEmpty()
                                    && !book.getBookCategories().isEmpty()
                                    && book.getPublisher() != null
                                    && !book.getBookAuthors().isEmpty()) {
                                books.add(book);
                            }

                        } catch (Exception e) {
                            // 개별 라인 파싱 실패 시 로그만 남기고 계속 진행
                            log.error("CSV Parsing Error at line with ISBN {}: {}", isbnThirteenNo, e.getMessage());
                        }
                    });
        }

        return books;
    }
}