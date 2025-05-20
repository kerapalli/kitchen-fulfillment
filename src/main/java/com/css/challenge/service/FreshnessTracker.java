package com.css.challenge.service;

import com.css.challenge.domain.Order;
import com.css.challenge.domain.Temperature;

import java.util.List;
import java.util.Map;

/**
 * Interface for tracking order freshness.
 */
public interface FreshnessTracker {

    /**
     * Updates the tracking status for an order in a specific storage temperature.
     *
     * @param order The order to track
     * @param storageTemp The temperature at which the order is stored
     */
    void trackOrder(Order order, Temperature storageTemp);

    /**
     * Removes an order from tracking.
     *
     * @param orderId The ID of the order to stop tracking
     */
    void stopTracking(String orderId);

    /**
     * Gets the remaining freshness for an order in milliseconds.
     *
     * @param orderId The ID of the order
     * @param currentTemp The current storage temperature
     * @return Remaining freshness in milliseconds, or 0 if expired/not found
     */
    long getRemainingFreshness(String orderId, Temperature currentTemp);

    /**
     * Checks if an order is expired.
     *
     * @param orderId The ID of the order
     * @param currentTemp The current storage temperature
     * @return true if expired, false otherwise
     */
    boolean isExpired(String orderId, Temperature currentTemp);

    /**
     * Gets all orders with their freshness values normalized to a value between 0 and 1.
     * A value closer to 0 means the order is close to expiration.
     *
     * @return Map of order IDs to normalized freshness values
     */
    Map<String, Double> getNormalizedFreshnessValues();

    /**
     * Gets orders sorted by their freshness values (most stale first).
     *
     * @param temperature The storage temperature to consider
     * @return List of orders sorted by freshness (ascending)
     */
    List<Order> getOrdersSortedByFreshness(Temperature temperature);
}