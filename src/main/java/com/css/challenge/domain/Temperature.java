package com.css.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the possible temperature requirements for food storage.
 */
public enum Temperature {
    HOT("hot"),
    COLD("cold"),
    ROOM("room");

    private final String value;

    Temperature(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static Temperature fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (Temperature temp : Temperature.values()) {
            if (temp.value.equalsIgnoreCase(value)) {
                return temp;
            }
        }

        throw new IllegalArgumentException("Unknown Temperature value: " + value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
