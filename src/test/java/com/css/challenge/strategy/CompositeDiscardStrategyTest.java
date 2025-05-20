package com.css.challenge.strategy;

import com.css.challenge.domain.Order;
import com.css.challenge.domain.Temperature;
import com.css.challenge.service.FreshnessTracker;
import com.css.challenge.storage.StorageUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit test for CompositeDiscardStrategy.
 */
public class CompositeDiscardStrategyTest {

    private CompositeDiscardStrategy strategy;
    @Mock
    private FreshnessTracker mockFreshnessTracker;
    @Mock
    private StorageUnit mockShelf;
    private Map<String, Order> shelfOrders;
    private Map<String, Double> freshnessValues;

    @Before
    public void setUp() {
        // Create strategy with 50% temperature mismatch penalty for clear testing
        strategy = new CompositeDiscardStrategy(0.5);

        // Create mocks
        mockFreshnessTracker = mock(FreshnessTracker.class);
        mockShelf = mock(StorageUnit.class);

        // Prepare test data
        shelfOrders = new HashMap<>();
        freshnessValues = new HashMap<>();

        // Set up mock shelf to return ROOM temperature
        when(mockShelf.getTemperature()).thenReturn(Temperature.ROOM);
    }

    @Test
    public void testSelectsLeastFreshOrderWithTemperaturePenalty() {
        // Create test orders
        Order hotOrder = new Order("hot1", "Hot Pizza", Temperature.HOT, 120);
        Order coldOrder = new Order("cold1", "Ice Cream", Temperature.COLD, 60);
        Order roomOrder = new Order("room1", "Sandwich", Temperature.ROOM, 600);

        // Add orders to shelf
        shelfOrders.put("hot1", hotOrder);
        shelfOrders.put("cold1", coldOrder);
        shelfOrders.put("room1", roomOrder);

        // Set up freshness values (0.0 = about to expire, 1.0 = fresh)
        // Without temperature penalty:
        // - hot1 would have 0.4 (second to expire)
        // - cold1 would have 0.3 (first to expire)
        // - room1 would have 0.8 (last to expire)
        freshnessValues.put("hot1", 0.4);
        freshnessValues.put("cold1", 0.3);
        freshnessValues.put("room1", 0.8);

        // Configure mocks
        when(mockFreshnessTracker.getNormalizedFreshnessValues()).thenReturn(freshnessValues);

        // With 50% temperature penalty:
        // - hot1 becomes 0.4 * 0.5 = 0.2 (first to expire)
        // - cold1 becomes 0.3 * 0.5 = 0.15 (now first to expire with penalty)
        // - room1 stays at 0.8 (last to expire)
        String result = strategy.selectOrderToDiscard(shelfOrders, mockShelf, mockFreshnessTracker);

        // Cold order should be selected due to lowest adjusted freshness
        assertEquals("cold1", result);

        // Verify that the freshness tracker was called
        verify(mockFreshnessTracker).getNormalizedFreshnessValues();
    }

    @Test
    public void testSelectsOrderWhenAllAtIdealTemperature() {
        // Create test orders, all at room temperature (ideal for shelf)
        Order order1 = new Order("room1", "Sandwich 1", Temperature.ROOM, 300);
        Order order2 = new Order("room2", "Sandwich 2", Temperature.ROOM, 600);
        Order order3 = new Order("room3", "Sandwich 3", Temperature.ROOM, 180);

        // Add orders to shelf
        shelfOrders.put("room1", order1);
        shelfOrders.put("room2", order2);
        shelfOrders.put("room3", order3);

        // Set up freshness values
        freshnessValues.put("room1", 0.5);
        freshnessValues.put("room2", 0.9);
        freshnessValues.put("room3", 0.2); // Least fresh

        // Configure mocks
        when(mockFreshnessTracker.getNormalizedFreshnessValues()).thenReturn(freshnessValues);

        // Execute strategy
        String result = strategy.selectOrderToDiscard(shelfOrders, mockShelf, mockFreshnessTracker);

        // Least fresh order should be selected since no temperature penalty applies
        assertEquals("room3", result);
    }

    @Test
    public void testHandlesEmptyFreshnessValues() {
        // Create test orders
        Order hotOrder = new Order("hot1", "Hot Pizza", Temperature.HOT, 120);
        Order coldOrder = new Order("cold1", "Ice Cream", Temperature.COLD, 60);

        // Add orders to shelf
        shelfOrders.put("hot1", hotOrder);
        shelfOrders.put("cold1", coldOrder);

        // Return empty freshness values (simulating new orders not yet tracked)
        when(mockFreshnessTracker.getNormalizedFreshnessValues()).thenReturn(new HashMap<>());

        // Execute strategy
        String result = strategy.selectOrderToDiscard(shelfOrders, mockShelf, mockFreshnessTracker);

        // Should not crash and should return one of the orders
        // (In our implementation, with 0.0 default freshness, it would pick either hot1 or cold1
        // depending on which one is processed first, but both would have the same adjusted score of 0.0)
        assertTrue(result.equals("hot1") || result.equals("cold1"));
    }
}