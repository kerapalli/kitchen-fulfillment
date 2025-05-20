package com.css.challenge.strategy;

import com.css.challenge.domain.Order;
import com.css.challenge.service.FreshnessTracker;
import com.css.challenge.storage.StorageUnit;

import java.util.Map;

/**
 * Interface defining a strategy for selecting orders to discard.
 * Different implementations can provide different selection criteria.
 */
public interface DiscardStrategy {

    /**
     * Selects an order to be discarded from the shelf.
     *
     * @param shelfOrders Map of order IDs to orders currently on the shelf
     * @param shelf The shelf storage unit
     * @param freshnessTracker Tracker to access freshness information
     * @return ID of the selected order to discard
     * @throws IllegalStateException if no order can be selected
     */
    String selectOrderToDiscard(
            Map<String, Order> shelfOrders,
            StorageUnit shelf,
            FreshnessTracker freshnessTracker);
}