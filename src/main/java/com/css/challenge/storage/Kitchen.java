package com.css.challenge.storage;

import com.css.challenge.domain.Order;
import com.css.challenge.domain.StorageType;
import com.css.challenge.domain.Temperature;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents the kitchen with its storage units.
 * Thread-safe implementation to handle concurrent order operations.
 */
public class Kitchen {
    // Storage unit for hot orders
    private final StorageUnit heater;

    // Storage unit for cold orders
    private final StorageUnit cooler;

    // Storage unit for any orders at room temperature
    private final StorageUnit shelf;

    // Lock for complex operations involving multiple storage units
    private final ReadWriteLock kitchenLock = new ReentrantReadWriteLock();

    // Map to quickly locate orders
    private final Map<String, StorageUnit> orderLocations = new ConcurrentHashMap<>();

    public Kitchen(int heaterCapacity, int coolerCapacity, int shelfCapacity) {
        this.heater = new StorageUnit(StorageType.HEATER, Temperature.HOT, heaterCapacity);
        this.cooler = new StorageUnit(StorageType.COOLER, Temperature.COLD, coolerCapacity);
        this.shelf = new StorageUnit(StorageType.SHELF, Temperature.ROOM, shelfCapacity);
    }

    /**
     * Creates a new kitchen with default capacities from requirements.
     */
    public Kitchen() {
        this(6, 6, 12);
    }

    /**
     * Gets the storage unit that is ideal for an order based on its temperature requirements.
     */
    public StorageUnit getIdealStorageUnit(Order order) {
        return switch (order.getTemp()) {
            case HOT -> heater;
            case COLD -> cooler;
            default -> shelf;
        };
    }

    /**
     * Gets a storage unit by its type.
     */
    public StorageUnit getStorageUnit(StorageType type) {
        return switch (type) {
            case HEATER -> heater;
            case COOLER -> cooler;
            default -> shelf;
        };
    }

    /**
     * Finds the storage unit containing an order.
     */
    public Optional<StorageUnit> findStorageUnitForOrder(String orderId) {
        // Quick lookup first
        StorageUnit unit = orderLocations.get(orderId);
        if (unit != null && unit.containsOrder(orderId)) {
            return Optional.of(unit);
        }

        // Fall back to full search if quick lookup fails
        kitchenLock.readLock().lock();
        try {
            if (heater.containsOrder(orderId)) {
                return Optional.of(heater);
            }
            if (cooler.containsOrder(orderId)) {
                return Optional.of(cooler);
            }
            if (shelf.containsOrder(orderId)) {
                return Optional.of(shelf);
            }
            return Optional.empty();
        } finally {
            kitchenLock.readLock().unlock();
        }
    }

    /**
     * Retrieves an order from the kitchen.
     */
    public Optional<Order> getOrder(String orderId) {
        return findStorageUnitForOrder(orderId)
                .flatMap(unit -> unit.getOrder(orderId));
    }

    /**
     * Stores an order in the kitchen.
     * @return true if the order was stored successfully, false otherwise
     */
    public boolean storeOrder(Order order, StorageUnit unit) {
        boolean stored = unit.storeOrder(order);
        if (stored) {
            orderLocations.put(order.getId(), unit);
        }
        return stored;
    }

    /**
     * Removes an order from the kitchen.
     *
     * @param orderId The ID of the order to remove
     * @return The removed order, or empty if not found
     */
    public Optional<Order> removeOrder(String orderId) {
        Optional<StorageUnit> unitOpt = findStorageUnitForOrder(orderId);
        if (unitOpt.isEmpty()) {
            return Optional.empty();
        }

        StorageUnit unit = unitOpt.get();
        Optional<Order> orderOpt = unit.removeOrder(orderId);
        if (orderOpt.isPresent()) {
            orderLocations.remove(orderId);
        }
        return orderOpt;
    }

    /**
     * Moves an order from one storage unit to another.
     *
     * @param orderId The ID of the order to move
     * @param sourceUnit The source storage unit
     * @param targetUnit The target storage unit
     * @return true if the move was successful, false otherwise
     */
    public boolean moveOrder(String orderId, StorageUnit sourceUnit, StorageUnit targetUnit) {
        kitchenLock.writeLock().lock();
        try {
            // Check if the order exists in the source unit
            Optional<Order> orderOpt = sourceUnit.getOrder(orderId);
            if (orderOpt.isEmpty()) {
                return false;
            }

            // Check if the target unit has capacity
            if (!targetUnit.hasCapacity()) {
                return false;
            }

            // Remove from source and add to target
            Order order = orderOpt.get();
            sourceUnit.removeOrder(orderId);
            boolean stored = targetUnit.storeOrder(order);

            // Update location map
            if (stored) {
                orderLocations.put(orderId, targetUnit);
                return true;
            } else {
                // Rollback if target storage failed
                sourceUnit.storeOrder(order);
                return false;
            }
        } finally {
            kitchenLock.writeLock().unlock();
        }
    }

    public Map<String, Order> getShelfOrders() {
        return shelf.getAllOrders();
    }

    public StorageUnit getHeater() {
        return heater;
    }

    public StorageUnit getCooler() {
        return cooler;
    }

    public StorageUnit getShelf() {
        return shelf;
    }
}