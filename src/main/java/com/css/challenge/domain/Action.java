package com.css.challenge.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Action is a json-friendly representation of a kitchen actionType.
 * It provides a structured way to record actions performed on an order.
 */
public class Action {
    private final long timestamp; // unix timestamp in microseconds
    private final String id; // order id
    private final String actionType; // place, move, pickup or discard

    /**
     * Creates a new actionType using an ActionType enum.
     *
     * @param timestamp Timestamp when the actionType occurred
     * @param id Order ID the actionType applies to
     * @param actionType Type of actionType performed
     */
    public Action(Instant timestamp, String id, ActionType actionType) {
        this.timestamp = ChronoUnit.MICROS.between(Instant.EPOCH, timestamp);
        this.id = id;
        this.actionType = actionType.name().toLowerCase();
    }

    /**
     * Creates a new actionType using a string actionType.
     * This constructor is provided for backward compatibility and JSON deserialization.
     *
     * @param timestamp Timestamp when the actionType occurred
     * @param id Order ID the actionType applies to
     * @param actionType String representation of the actionType
     */
    public Action(Instant timestamp, String id, String actionType) {
        this.timestamp = ChronoUnit.MICROS.between(Instant.EPOCH, timestamp);
        this.id = id;
        this.actionType = actionType;
    }

    /**
     * Gets the actionType timestamp in microseconds since epoch.
     *
     * @return Timestamp in microseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the order ID.
     *
     * @return Order ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the actionType as a string.
     *
     * @return String representation of the actionType
     */
    public String getActionType() {
        return actionType;
    }


    @Override
    public String toString() {
        return "{timestamp: " + timestamp + ", id: " + id + ", actionType: " + actionType + " }";
    }
}