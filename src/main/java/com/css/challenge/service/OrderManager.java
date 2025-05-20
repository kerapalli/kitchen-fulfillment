package com.css.challenge.service;

import com.css.challenge.domain.Action;
import com.css.challenge.domain.Order;

import java.util.List;
import java.util.Optional;

/**
 * Interface for managing orders in the kitchen.
 */
public interface OrderManager {

    /**
     * Places a new order in the kitchen.
     * Follows the placement logic from the requirements:
     * 1. Try to store at ideal temperature
     * 2. If full, place on shelf
     * 3. If shelf full, move an order and then place, or discard if necessary
     *
     * @param order The order to place
     * @return The Action that was performed
     */
    Action placeOrder(Order order);

    /**
     * Moves an order from one storage unit to another.
     *
     * @param orderId The ID of the order to move
     * @param sourceUnitType Source storage unit
     * @param targetUnitType Target storage unit
     * @return true if the move was successful, false otherwise
     */
    boolean moveOrder(String orderId, String sourceUnitType, String targetUnitType);

    /**
     * Picks up an order from the kitchen.
     *
     * @param orderId The ID of the order to pick up
     * @return The picked up order, or empty if not found
     */
    Optional<Order> pickupOrder(String orderId);

    /**
     * Discards an order from the kitchen.
     *
     * @param orderId The ID of the order to discard
     * @return true if an order was discarded, false if not found
     */
    boolean discardOrder(String orderId);

    /**
     * Gets all actions that have been performed in the kitchen.
     *
     * @return List of all actions
     */
    List<Action> getAllActions();

    /**
     * Checks if the order exists in any storage unit.
     *
     * @param orderId The ID of the order to check
     * @return true if the order exists, false otherwise
     */
    boolean orderExists(String orderId);

    /**
     * Gets the location of an order.
     *
     * @param orderId The ID of the order to locate
     * @return The storage unit name if found, or empty if not found
     */
    Optional<String> getOrderLocation(String orderId);
}