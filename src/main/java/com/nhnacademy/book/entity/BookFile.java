package com.nhnacademy.book.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "File")
public class BookFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType; // 리뷰 파일인지? 도서 파일인지?

    @Column(nullable = false)
    private Long joinedId; //리뷰라면? 리뷰 id 가, book 이라면 book id 가.

    @Builder
    public BookFile(String fileUrl, FileType fileType, Long joinedId) {
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.joinedId = joinedId;
    }

}
