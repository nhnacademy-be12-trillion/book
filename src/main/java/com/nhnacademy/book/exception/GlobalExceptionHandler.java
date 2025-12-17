package com.nhnacademy.book.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BookNotFoundException.class,
    CategoryNotFoundException.class, MemberNotFoundException.class,
    WishlistNotFoundException.class, ReviewNotFoundException.class})
    public ResponseEntity<ExceptionResponse> handleNotFoundException(Exception e){
        log.warn("DATA_NOT_FOUND");
        ExceptionResponse response = ExceptionResponse.of(
                HttpStatus.NOT_FOUND.toString(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({AlreadyEnrolledException.class,
    StockNotEnoughException.class})
    public ResponseEntity<ExceptionResponse> handleAlreadyEnrolledException(AlreadyEnrolledException e){
        log.warn("STATE_CONFLICT");
        ExceptionResponse response = ExceptionResponse.of(
                HttpStatus.CONFLICT.toString(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ReviewAccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleReviewAccessDeniedException(ReviewAccessDeniedException e){
        log.warn("ACCESS_DENIED");
        ExceptionResponse response = ExceptionResponse.of(
                HttpStatus.FORBIDDEN.toString(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ExternalApiCallException.class)
    public ResponseEntity<ExceptionResponse> handleExternalApiCallException(ExternalApiCallException e){
        log.warn("EXTERNAL_API_ERROR");
        ExceptionResponse response = ExceptionResponse.of(
                HttpStatus.BAD_GATEWAY.toString(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }


}
