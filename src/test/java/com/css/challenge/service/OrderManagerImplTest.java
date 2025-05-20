package com.css.challenge.service;

import com.css.challenge.domain.Action;
import com.css.challenge.domain.ActionType;
import com.css.challenge.domain.Order;
import com.css.challenge.domain.Temperature;
import com.css.challenge.storage.Kitchen;
import com.css.challenge.storage.StorageUnit;
import com.css.challenge.strategy.DiscardStrategy;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OrderManagerImplTest {

    private Kitchen mockKitchen;
    private ActionLogger mockActionLogger;
    private FreshnessTracker mockFreshnessTracker;
    private DiscardStrategy mockDiscardStrategy;
    private OrderManagerImpl orderManager;

    private StorageUnit mockHeater;
    private StorageUnit mockCooler;
    private StorageUnit mockShelf;

    @Before
    public void setUp() {
        mockKitchen = mock(Kitchen.class);
        mockActionLogger = mock(ActionLogger.class);
        mockFreshnessTracker = mock(FreshnessTracker.class);
        mockDiscardStrategy = mock(DiscardStrategy.class);

        mockHeater = mock(StorageUnit.class);
        mockCooler = mock(StorageUnit.class);
        mockShelf = mock(StorageUnit.class);

        when(mockHeater.getTemperature()).thenReturn(Temperature.HOT);
        when(mockCooler.getTemperature()).thenReturn(Temperature.COLD);
        when(mockShelf.getTemperature()).thenReturn(Temperature.ROOM);

        when(mockKitchen.getHeater()).thenReturn(mockHeater);
        when(mockKitchen.getCooler()).thenReturn(mockCooler);
        when(mockKitchen.getShelf()).thenReturn(mockShelf);

        orderManager = new OrderManagerImpl(mockKitchen, mockActionLogger, mockFreshnessTracker, mockDiscardStrategy);
    }

    @Test
    public void testPlaceOrderInIdealStorageUnit() {
        // Create a hot order
        Order hotOrder = new Order("hot1", "Pizza", Temperature.HOT, 120);

        // Mock behavior: ideal unit has capacity
        when(mockKitchen.getIdealStorageUnit(hotOrder)).thenReturn(mockHeater);
        when(mockKitchen.storeOrder(hotOrder, mockHeater)).thenReturn(true);

        // Mock the action logger to return a specific action
        Action placeAction = new Action(Instant.now(), hotOrder.getId(), ActionType.PLACE);
        when(mockActionLogger.logAction(hotOrder.getId(), ActionType.PLACE)).thenReturn(placeAction);

        // Place the order
        Action result = orderManager.placeOrder(hotOrder);

        // Verify the order was stored in the ideal unit
        verify(mockKitchen).storeOrder(hotOrder, mockHeater);
        verify(mockFreshnessTracker).trackOrder(hotOrder, mockHeater.getTemperature());
        verify(mockActionLogger).logAction(hotOrder.getId(), ActionType.PLACE);

        // Verify the result
        assertEquals(placeAction, result);
    }

    @Test
    public void testPlaceOrderWhenIdealUnitIsFullUsesShelf() {
        // Create a hot order
        Order hotOrder = new Order("hot1", "Pizza", Temperature.HOT, 120);

        // Mock behavior: ideal unit is full, shelf has capacity
        when(mockKitchen.getIdealStorageUnit(hotOrder)).thenReturn(mockHeater);
        when(mockKitchen.storeOrder(hotOrder, mockHeater)).thenReturn(false);
        when(mockShelf.hasCapacity()).thenReturn(true);
        when(mockKitchen.storeOrder(hotOrder, mockShelf)).thenReturn(true);

        // Mock the action logger to return a specific action
        Action placeAction = new Action(Instant.now(), hotOrder.getId(), ActionType.PLACE);
        when(mockActionLogger.logAction(hotOrder.getId(), ActionType.PLACE)).thenReturn(placeAction);

        // Place the order
        Action result = orderManager.placeOrder(hotOrder);

        // Verify the order was stored on the shelf
        verify(mockKitchen).storeOrder(hotOrder, mockHeater);
        verify(mockKitchen).storeOrder(hotOrder, mockShelf);
        verify(mockFreshnessTracker).trackOrder(hotOrder, mockShelf.getTemperature());
        verify(mockActionLogger).logAction(hotOrder.getId(), ActionType.PLACE);

        // Verify the result
        assertEquals(placeAction, result);
    }

    @Test
    public void testPlaceOrderWhenAllUnitsFullDiscardsOrder() {
        // Create orders with Instant
        Order hotOrder = new Order("hot1", "Pizza", Temperature.HOT, 120);
        Order oldOrder = new Order("old1", "Old Sandwich", Temperature.ROOM, 60);

        // Mock behavior: all units are full
        when(mockKitchen.getIdealStorageUnit(hotOrder)).thenReturn(mockHeater);
        when(mockKitchen.storeOrder(hotOrder, mockHeater)).thenReturn(false);
        when(mockShelf.hasCapacity()).thenReturn(false);

        // Mock the shelf orders
        Map<String, Order> shelfOrders = new HashMap<>();
        shelfOrders.put("old1", oldOrder);
        when(mockShelf.getAllOrders()).thenReturn(shelfOrders);

        // Mock move attempt to fail
        when(mockHeater.hasCapacity()).thenReturn(false);
        when(mockCooler.hasCapacity()).thenReturn(false);

        // Mock discard strategy to select the old order
        when(mockDiscardStrategy.selectOrderToDiscard(shelfOrders, mockShelf, mockFreshnessTracker))
                .thenReturn("old1");

        // Mock order removal and subsequent placement
        when(mockKitchen.removeOrder("old1")).thenReturn(Optional.of(oldOrder));
        when(mockKitchen.storeOrder(hotOrder, mockShelf)).thenReturn(true);

        // Mock actions
        Action discardAction = new Action(Instant.now(), "old1", ActionType.DISCARD);
        Action placeAction = new Action(Instant.now(), hotOrder.getId(), ActionType.PLACE);
        when(mockActionLogger.logAction("old1", ActionType.DISCARD)).thenReturn(discardAction);
        when(mockActionLogger.logAction(hotOrder.getId(), ActionType.PLACE)).thenReturn(placeAction);

        // Place the order
        Action result = orderManager.placeOrder(hotOrder);

        // Verify the old order was discarded
        verify(mockKitchen).removeOrder("old1");
        verify(mockFreshnessTracker).stopTracking("old1");
        verify(mockActionLogger).logAction("old1", ActionType.DISCARD);

        // Verify the new order was stored
        verify(mockKitchen).storeOrder(hotOrder, mockShelf);
        verify(mockFreshnessTracker).trackOrder(hotOrder, mockShelf.getTemperature());
        verify(mockActionLogger).logAction(hotOrder.getId(), ActionType.PLACE);

        // Verify the result
        assertEquals(placeAction, result);
    }

    @Test
    public void testPickupOrder() {
        // Create an order with Instant
        Order order = new Order("test1", "Test Order", Temperature.ROOM, 300);

        // Mock behavior
        when(mockKitchen.removeOrder("test1")).thenReturn(Optional.of(order));

        // Mock action
        Action pickupAction = new Action(Instant.now(), "test1", ActionType.PICKUP);
        when(mockActionLogger.logAction("test1", ActionType.PICKUP)).thenReturn(pickupAction);

        // Pickup the order
        Optional<Order> result = orderManager.pickupOrder("test1");

        // Verify the order was removed and tracked
        verify(mockKitchen).removeOrder("test1");
        verify(mockFreshnessTracker).stopTracking("test1");
        verify(mockActionLogger).logAction("test1", ActionType.PICKUP);

        // Verify the result
        assertTrue(result.isPresent());
        assertEquals(order, result.get());
    }

    @Test
    public void testPickupNonExistentOrder() {
        // Mock behavior: order doesn't exist
        when(mockKitchen.removeOrder("nonexistent")).thenReturn(Optional.empty());

        // Pickup the order
        Optional<Order> result = orderManager.pickupOrder("nonexistent");

        // Verify the order was attempted to be removed
        verify(mockKitchen).removeOrder("nonexistent");

        // Verify no tracking or logging occurred
        verify(mockFreshnessTracker, never()).stopTracking("nonexistent");
        verify(mockActionLogger, never()).logAction(eq("nonexistent"), any(ActionType.class));

        // Verify the result
        assertFalse(result.isPresent());
    }
}