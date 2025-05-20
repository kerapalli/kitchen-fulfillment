package com.css.challenge.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Order is a json-friendly representation of a food order.
 */
public class Order {
    private final String id; // order id
    private final String name; // food name
    private final Temperature temp; // ideal temperature
    private final int freshness; // freshness in seconds
    private final Instant placementTime; // when the order was placed

    /**
     * Creates a new order with the current time as placement time.
     *
     * @param id Order identifier
     * @param name Name of the food item
     * @param temp Ideal storage temperature
     * @param freshness Freshness duration in seconds
     */
    public Order(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("temp") Temperature temp,
            @JsonProperty("freshness") int freshness) {
        this.id = id;
        this.name = name;
        this.temp = temp;
        this.freshness = freshness;
        this.placementTime = Instant.now();
    }

    /**
     * Parses a JSON string into a list of orders.
     *
     * @param json JSON string to parse
     * @return List of orders
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static List<Order> parse(String json) throws JsonProcessingException {
        return new ObjectMapper().readValue(json, new TypeReference<List<Order>>() {});
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }


    public Temperature getTemp() {
        return temp;
    }

    public int getFreshness() {
        return freshness;
    }

    /**
     * Calculates the remaining freshness duration for this order based on the current storage temperature.
     * If the order is not stored at its ideal temperature, the freshness duration is halved.
     */
    public long getRemainingFreshness(Temperature currentTemp) {
        long elapsedMillis = Instant.now().toEpochMilli() - placementTime.toEpochMilli();
        long freshnessDuration = freshness * 1000L; // convert to milliseconds

        // If not at ideal temperature, freshness is halved
        if (currentTemp != temp) {
            freshnessDuration /= 2;
        }

        return Math.max(0, freshnessDuration - elapsedMillis);
    }

    /**
     * Determines if the order has expired based on storage temperature and elapsed time.
     *
     * @param currentTemp The temperature at which the order is currently stored
     * @return true if the order is expired, false otherwise
     */
    public boolean isExpired(Temperature currentTemp) {
        return getRemainingFreshness(currentTemp) <= 0;
    }

    @Override
    public String toString() {
        return "{id: " + id + ", name: " + name + ", temp: " + temp + ", freshness:" + freshness + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}