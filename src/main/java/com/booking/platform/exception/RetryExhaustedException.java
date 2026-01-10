package com.booking.platform.exception;

public class RetryExhaustedException extends RuntimeException {
    public RetryExhaustedException(String message) {
        super(message);
    }
}