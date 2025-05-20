package com.css.challenge.strategy;

import com.css.challenge.domain.Order;
import com.css.challenge.service.FreshnessTracker;
import com.css.challenge.storage.StorageUnit;

import java.util.Map;

/**
 * Strategy for selecting orders to discard based on freshness.
 * Orders closest to expiry will be selected first.
 */
public class FreshnessDiscardStrategy implements DiscardStrategy {

    @Override
    public String selectOrderToDiscard(
            Map<String, Order> shelfOrders,
            StorageUnit shelf,
            FreshnessTracker freshnessTracker) {

        if (shelfOrders.isEmpty()) {
            throw new IllegalStateException("Cannot select order to discard: shelf is empty");
        }

        // Get normalized freshness values (0 = about to expire, 1 = fresh)
        Map<String, Double> freshness = freshnessTracker.getNormalizedFreshnessValues();

        // Find the order with the lowest freshness value
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