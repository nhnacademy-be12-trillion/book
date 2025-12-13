package com.nhnacademy.book.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BookNotFoundException.class,
    CategoryNotFoundException.class, MemberNotFoundException.class,
    WishlistNotFoundException.class, ReviewNotFoundException.class})
    public ResponseEntity<ExceptionResponse> handleNotFoundException(Exception e){
        ExceptionResponse response = ExceptionResponse.of(
                "DATA_NOT_FOUND",
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({AlreadyEnrolledException.class,
    NotEnoughStockException.class})
    public ResponseEntity<ExceptionResponse> handleAlreadyEnrolledException(AlreadyEnrolledException e){
        ExceptionResponse response = ExceptionResponse.of(
                "STATE_CONFLICT",
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ReviewAccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleReviewAccessDeniedException(ReviewAccessDeniedException e){
        ExceptionResponse response = ExceptionResponse.of(
                "ACCESS_DENIED",
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ExternalApiCallException.class)
    public ResponseEntity<ExceptionResponse> handleExternalApiCallException(ExternalApiCallException e){
        ExceptionResponse response = ExceptionResponse.of(
                "EXTERNAL_API_ERROR",
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }


}
