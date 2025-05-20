package com.css.challenge.service;

import com.css.challenge.domain.Order;
import com.css.challenge.domain.Temperature;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the FreshnessTracker interface.
 * Thread-safe to handle concurrent order operations.
 */
public class FreshnessTrackerImpl implements FreshnessTracker {
    // Map of order ID to order
    private final Map<String, Order> trackedOrders;

    // Map of order ID to current storage temperature
    private final Map<String, Temperature> orderTemperatures;

    /**
     * Creates a new freshness tracker.
     */
    public FreshnessTrackerImpl() {
        this.trackedOrders = new ConcurrentHashMap<>();
        this.orderTemperatures = new ConcurrentHashMap<>();
    }

    @Override
    public void trackOrder(Order order, Temperature storageTemp) {
        trackedOrders.put(order.getId(), order);
        orderTemperatures.put(order.getId(), storageTemp);
    }

    @Override
    public void stopTracking(String orderId) {
        trackedOrders.remove(orderId);
        orderTemperatures.remove(orderId);
    }

    @Override
    public long getRemainingFreshness(String orderId, Temperature currentTemp) {
        Order order = trackedOrders.get(orderId);
        if (order == null) {
            return 0;
        }
        return order.getRemainingFreshness(currentTemp);
    }

    @Override
    public boolean isExpired(String orderId, Temperature currentTemp) {
        Order order = trackedOrders.get(orderId);
        if (order == null) {
            return true; // Non-existent orders are considered expired
        }
        return order.isExpired(currentTemp);
    }

    @Override
    public Map<String, Double> getNormalizedFreshnessValues() {
        Map<String, Double> normalizedValues = new HashMap<>();

        for (Map.Entry<String, Order> entry : trackedOrders.entrySet()) {
            String orderId = entry.getKey();
            Order order = entry.getValue();
            Temperature currentTemp = orderTemperatures.getOrDefault(orderId, Temperature.ROOM);

            // Get freshness in milliseconds
            long remainingFreshness = order.getRemainingFreshness(currentTemp);

            // Calculate original total freshness in milliseconds
            long totalFreshness;
            if (currentTemp == order.getTemp()) {
                totalFreshness = order.getFreshness() * 1000L;
            } else {
                totalFreshness = (order.getFreshness() * 1000L) / 2;
            }

            // Normalize to a value between 0 and 1
            double normalizedValue = (double) remainingFreshness / totalFreshness;
            normalizedValues.put(orderId, Math.max(0.0, Math.min(1.0, normalizedValue)));
        }

        return normalizedValues;
    }

    @Override
    public List<Order> getOrdersSortedByFreshness(Temperature temperature) {
        // Get all orders that are currently tracked
        return trackedOrders.values().stream()
                // Sort by remaining freshness (ascending)
                .sorted(Comparator.comparingLong(order ->
                        order.getRemainingFreshness(temperature)))
                .collect(Collectors.toList());
    }
}