package com.nhnacademy.book.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundBookIndexException.class)
    public ResponseEntity<ExceptionResponse> handleNotFoundBookIndexException(NotFoundBookIndexException e){
        ExceptionResponse response = ExceptionResponse.of(
                "INDEX_NOT_FOUND",
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

    }
}
