package com.css.challenge.strategy;

import com.css.challenge.domain.Order;
import com.css.challenge.service.FreshnessTracker;
import com.css.challenge.storage.StorageUnit;

import java.util.Map;

/**
 * Strategy for selecting orders to discard based on temperature mismatch.
 * Orders not at their ideal temperature will be selected first.
 */
public class TemperatureMismatchDiscardStrategy implements DiscardStrategy {

    @Override
    public String selectOrderToDiscard(
            Map<String, Order> shelfOrders,
            StorageUnit shelf,
            FreshnessTracker freshnessTracker) {

        if (shelfOrders.isEmpty()) {
            throw new IllegalStateException("Cannot select order to discard: shelf is empty");
        }

        // First priority: orders not at ideal temperature
        for (Map.Entry<String, Order> entry : shelfOrders.entrySet()) {
            Order order = entry.getValue();
            if (shelf.getTemperature() != order.getTemp()) {
                return entry.getKey();
            }
        }

        // If all orders are at ideal temperature, fall back to least fresh
        Map<String, Double> freshness = freshnessTracker.getNormalizedFreshnessValues();

        String orderToDiscard = null;
        double lowestFreshness = Double.MAX_VALUE;

        for (String orderId : shelfOrders.keySet()) {
            double freshnessValue = freshness.getOrDefault(orderId, 0.0);

            if (freshnessValue < lowestFreshness) {
                lowestFreshness = freshnessValue;
                orderToDiscard = orderId;
            }
        }

        return orderToDiscard;
    }
}