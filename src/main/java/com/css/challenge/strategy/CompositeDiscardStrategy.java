package com.css.challenge.strategy;

import com.css.challenge.domain.Order;
import com.css.challenge.service.FreshnessTracker;
import com.css.challenge.storage.StorageUnit;

import java.util.Map;

/**
 * A composite strategy that combines temperature mismatch and freshness.
 * Orders not at ideal temperature are penalized in freshness calculation.
 * This is the default strategy used by the system.
 */
public class CompositeDiscardStrategy implements DiscardStrategy {

    // Weight factors for different criteria
    private final double temperatureMismatchPenalty;

    /**
     * Creates a new composite discard strategy.
     *
     * @param temperatureMismatchPenalty Penalty factor for orders not at ideal temperature (0.0-1.0)
     */
    public CompositeDiscardStrategy(double temperatureMismatchPenalty) {
        this.temperatureMismatchPenalty = Math.max(0.0, Math.min(1.0, temperatureMismatchPenalty));
    }

    /**
     * Creates a new composite discard strategy with default penalty.
     */
    public CompositeDiscardStrategy() {
        this(0.2); // Default 20% penalty
    }

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

        // Find the order with the lowest adjusted freshness value
        String orderToDiscard = null;
        double lowestAdjustedFreshness = Double.MAX_VALUE;

        for (String orderId : shelfOrders.keySet()) {
            Order order = shelfOrders.get(orderId);
            double freshnessValue = freshness.getOrDefault(orderId, 0.0);

            // Apply temperature mismatch penalty
            if (shelf.getTemperature() != order.getTemp()) {
                freshnessValue *= (1.0 - temperatureMismatchPenalty);
            }

            if (freshnessValue < lowestAdjustedFreshness) {
                lowestAdjustedFreshness = freshnessValue;
                orderToDiscard = orderId;
            }
        }

        return orderToDiscard;
    }
}