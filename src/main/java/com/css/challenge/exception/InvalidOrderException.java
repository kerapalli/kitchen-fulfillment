package com.css.challenge.exception;

/**
 * Exception thrown when an order is invalid.
 */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}