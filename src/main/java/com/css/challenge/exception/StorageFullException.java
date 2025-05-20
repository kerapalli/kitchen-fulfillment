package com.css.challenge.exception;

import com.css.challenge.domain.StorageType;

/**
 * Exception thrown when a storage unit is full and cannot accept more orders.
 */
public class StorageFullException extends RuntimeException {

    public StorageFullException(StorageType storageType) {
        super("Storage unit is full: " + storageType);
    }
}