package com.css.challenge.exception;

/**
 * Exception thrown when an order is not found in the kitchen.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}