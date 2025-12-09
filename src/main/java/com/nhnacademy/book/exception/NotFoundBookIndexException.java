package com.nhnacademy.book.exception;

public class NotFoundBookIndexException extends RuntimeException {
    public NotFoundBookIndexException(String message) {
        super(message);
    }
}
