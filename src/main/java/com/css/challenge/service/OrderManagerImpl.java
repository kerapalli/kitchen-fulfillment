package com.css.challenge.service;

import com.css.challenge.domain.Action;
import com.css.challenge.domain.ActionType;
import com.css.challenge.domain.Order;
import com.css.challenge.domain.StorageType;
import com.css.challenge.domain.Temperature;
import com.css.challenge.storage.Kitchen;
import com.css.challenge.storage.StorageUnit;
import com.css.challenge.strategy.CompositeDiscardStrategy;
import com.css.challenge.strategy.DiscardStrategy;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the OrderManager interface.
 * Thread-safe to handle concurrent order operations.
 */
public class OrderManagerImpl implements OrderManager {
    private final Kitchen kitchen;
    private final ActionLogger actionLogger;
    private final FreshnessTracker freshnessTracker;
    private final DiscardStrategy discardStrategy;
    private final ReadWriteLock orderLock = new ReentrantReadWriteLock();

    public OrderManagerImpl(
            Kitchen kitchen,
            ActionLogger actionLogger,
            FreshnessTracker freshnessTracker,
            DiscardStrategy discardStrategy) {
        this.kitchen = kitchen;
        this.actionLogger = actionLogger;
        this.freshnessTracker = freshnessTracker;
        this.discardStrategy = discardStrategy;
    }

    /**
     * Creates a new order manager with the default discard strategy.
     */
    public OrderManagerImpl(Kitchen kitchen, ActionLogger actionLogger, FreshnessTracker freshnessTracker) {
        this(kitchen, actionLogger, freshnessTracker, new CompositeDiscardStrategy());
    }

    @Override
    public Action placeOrder(Order order) {
        orderLock.writeLock().lock();
        try {
            // First, try to store in ideal storage unit
            StorageUnit idealUnit = kitchen.getIdealStorageUnit(order);
            if (kitchen.storeOrder(order, idealUnit)) {
                freshnessTracker.trackOrder(order, idealUnit.getTemperature());
                return actionLogger.logAction(order.getId(), ActionType.PLACE);
            }

            // If ideal unit is full, try the shelf
            StorageUnit shelf = kitchen.getShelf();
            if (shelf.hasCapacity()) {
                kitchen.storeOrder(order, shelf);
                freshnessTracker.trackOrder(order, shelf.getTemperature());
                return actionLogger.logAction(order.getId(), ActionType.PLACE);
            }

            // If shelf is full, try to move an existing order from shelf to its ideal unit
            boolean foundSpaceOnShelf = tryMoveOrderFromShelf();

            if (foundSpaceOnShelf && shelf.hasCapacity()) {
                kitchen.storeOrder(order, shelf);
                freshnessTracker.trackOrder(order, shelf.getTemperature());
                return actionLogger.logAction(order.getId(), ActionType.PLACE);
            }

            // If still full, we need to discard an order based on our selection criteria
            String orderToDiscard = selectOrderToDiscard();
            discardOrder(orderToDiscard);

            // Now there should be space on the shelf
            kitchen.storeOrder(order, shelf);
            freshnessTracker.trackOrder(order, shelf.getTemperature());
            return actionLogger.logAction(order.getId(), ActionType.PLACE);

        } finally {
            orderLock.writeLock().unlock();
        }
    }

    /**
     * Tries to move an order from the shelf to its ideal storage unit.
     *
     * @return true if an order was moved successfully, false otherwise
     */
    private boolean tryMoveOrderFromShelf() {
        StorageUnit shelf = kitchen.getShelf();
        Map<String, Order> shelfOrders = shelf.getAllOrders();

        // Look for hot orders on the shelf that could go to the heater
        for (Map.Entry<String, Order> entry : shelfOrders.entrySet()) {
            if (entry.getValue().getTemp() == Temperature.HOT && kitchen.getHeater().hasCapacity()) {
                moveOrder(entry.getKey(), StorageType.SHELF.name(), StorageType.HEATER.name());
                return true;
            }
        }

        // Look for cold orders on the shelf that could go to the cooler
        for (Map.Entry<String, Order> entry : shelfOrders.entrySet()) {
            if (entry.getValue().getTemp() == Temperature.COLD && kitchen.getCooler().hasCapacity()) {
                moveOrder(entry.getKey(), StorageType.SHELF.name(), StorageType.COOLER.name());
                return true;
            }
        }

        return false;
    }

    /**
     * Selects an order to discard using the configured discard strategy.
     *
     * @return ID of the order to discard
     */
    private String selectOrderToDiscard() {
        StorageUnit shelf = kitchen.getShelf();
        Map<String, Order> shelfOrders = shelf.getAllOrders();

        return discardStrategy.selectOrderToDiscard(shelfOrders, shelf, freshnessTracker);
    }

    @Override
    public boolean moveOrder(String orderId, String sourceUnitType, String targetUnitType) {
        orderLock.writeLock().lock();
        try {
            StorageType sourceType = StorageType.valueOf(sourceUnitType);
            StorageType targetType = StorageType.valueOf(targetUnitType);

            StorageUnit sourceUnit = kitchen.getStorageUnit(sourceType);
            StorageUnit targetUnit = kitchen.getStorageUnit(targetType);

            boolean moved = kitchen.moveOrder(orderId, sourceUnit, targetUnit);

            if (moved) {
                // Update freshness tracking with new temperature
                Optional<Order> orderOpt = targetUnit.getOrder(orderId);
                orderOpt.ifPresent(order -> freshnessTracker.trackOrder(order, targetUnit.getTemperature()));

                // Log the move action
                actionLogger.logAction(orderId, ActionType.MOVE);
                return true;
            }

            return false;
        } finally {
            orderLock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Order> pickupOrder(String orderId) {
        orderLock.writeLock().lock();
        try {
            Optional<Order> orderOpt = kitchen.removeOrder(orderId);

            if (orderOpt.isPresent()) {
                freshnessTracker.stopTracking(orderId);
                actionLogger.logAction(orderId, ActionType.PICKUP);
            }

            return orderOpt;
        } finally {
            orderLock.writeLock().unlock();
        }
    }

    @Override
    public boolean discardOrder(String orderId) {
        orderLock.writeLock().lock();
        try {
            Optional<Order> orderOpt = kitchen.removeOrder(orderId);

            if (orderOpt.isPresent()) {
                freshnessTracker.stopTracking(orderId);
                actionLogger.logAction(orderId, ActionType.DISCARD);
                return true;
            }

            return false;
        } finally {
            orderLock.writeLock().unlock();
        }
    }

    @Override
    public List<Action> getAllActions() {
        return actionLogger.getAllActions();
    }

    @Override
    public boolean orderExists(String orderId) {
        return kitchen.findStorageUnitForOrder(orderId).isPresent();
    }

    @Override
    public Optional<String> getOrderLocation(String orderId) {
        Optional<StorageUnit> unitOpt = kitchen.findStorageUnitForOrder(orderId);
        return unitOpt.map(unit -> unit.getType().name());
    }
}