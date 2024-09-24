package io.hhplus.tdd.point.exception;

public class InvalidUserIdException extends RuntimeException {
    public InvalidUserIdException(String message) {
        super(message);
    }
}