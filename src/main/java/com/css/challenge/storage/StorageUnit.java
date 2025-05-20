package com.css.challenge.storage;

import com.css.challenge.domain.Order;
import com.css.challenge.domain.StorageType;
import com.css.challenge.domain.Temperature;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a storage unit in the kitchen with specific capacity and temperature characteristics.
 * Thread-safe implementation to handle concurrent order operations.
 */
public class StorageUnit {
    private final StorageType type;
    private final Temperature temperature;
    private final int capacity;
    private final Map<String, Order> orders;

    public StorageUnit(StorageType type, Temperature temperature, int capacity) {
        this.type = type;
        this.temperature = temperature;
        this.capacity = capacity;
        this.orders = new ConcurrentHashMap<>();
    }

    public boolean hasCapacity() {
        return orders.size() < capacity;
    }

    /**
     * Stores an order in this unit.
     */
    public boolean storeOrder(Order order) {
        if (!hasCapacity()) {
            return false;
        }
        orders.put(order.getId(), order);
        return true;
    }

    /**
     * Removes an order from this unit by its ID.
     */
    public Optional<Order> removeOrder(String orderId) {
        Order removedOrder = orders.remove(orderId);
        return Optional.ofNullable(removedOrder);
    }

    /**
     * Checks if this unit contains an order with the specified ID.
     */
    public boolean containsOrder(String orderId) {
        return orders.containsKey(orderId);
    }

    /**
     * Returns the order with the specified ID if it exists in this unit.
     */
    public Optional<Order> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Map<String, Order> getAllOrders() {
        return new ConcurrentHashMap<>(orders);
    }

    public int getOrderCount() {
        return orders.size();
    }

    public Temperature getTemperature() {
        return temperature;
    }

    public StorageType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }
}
