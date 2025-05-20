package com.css.challenge.client;

import com.css.challenge.domain.Action;
import com.css.challenge.domain.Order;
import com.css.challenge.service.ActionLogger;
import com.css.challenge.service.OrderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulator for processing orders in a kitchen.
 * Handles order placement and pickup based on configuration parameters.
 */
public class Simulator {
    // Default values from requirements
    private static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);
    public static final int DEFAULT_RATE_MS = 500; // 2 orders per second = 500ms interval
    public static final int DEFAULT_MIN_PICKUP_TIME_MS = 4000; // 4 seconds
    public static final int DEFAULT_MAX_PICKUP_TIME_MS = 8000; // 8 seconds

    private final List<Order> orders;
    private final OrderManager orderManager;
    private final ActionLogger actionLogger;
    private final int rateMs;
    private final int minPickupTimeMs;
    private final int maxPickupTimeMs;
    private final Random random;
    private final ScheduledExecutorService executor;

    // Track progress
    private final AtomicInteger ordersPlaced = new AtomicInteger(0);
    private final AtomicInteger ordersProcessed = new AtomicInteger(0);

    /**
     * Creates a new simulator with the specified parameters.
     *
     * @param orders The list of orders to process
     * @param orderManager The order manager to use
     * @param actionLogger The action logger to use
     * @param rateMs The rate at which to place orders (in milliseconds)
     * @param minPickupTimeMs The minimum time to wait before pickup (in milliseconds)
     * @param maxPickupTimeMs The maximum time to wait before pickup (in milliseconds)
     */
    public Simulator(
            List<Order> orders,
            OrderManager orderManager,
            ActionLogger actionLogger,
            int rateMs,
            int minPickupTimeMs,
            int maxPickupTimeMs) {
        this.orders = orders;
        this.orderManager = orderManager;
        this.actionLogger = actionLogger;
        this.rateMs = rateMs;
        this.minPickupTimeMs = minPickupTimeMs;
        this.maxPickupTimeMs = maxPickupTimeMs;
        this.random = new Random();
        this.executor = Executors.newScheduledThreadPool(
                Math.min(Runtime.getRuntime().availableProcessors() * 2, 10));
    }

    /**
     * Creates a new simulator with default parameters.
     *
     * @param orders The list of orders to process
     * @param orderManager The order manager to use
     * @param actionLogger The action logger to use
     */
    public Simulator(List<Order> orders, OrderManager orderManager, ActionLogger actionLogger) {
        this(orders, orderManager, actionLogger, DEFAULT_RATE_MS, DEFAULT_MIN_PICKUP_TIME_MS, DEFAULT_MAX_PICKUP_TIME_MS);
    }

    /**
     * Runs the simulation.
     *
     * @return List of actions performed during the simulation
     * @throws InterruptedException if interrupted while waiting for simulation to complete
     */
    public List<Action> run() throws InterruptedException {
        LOGGER.info("Starting simulation with {} orders...", orders.size());
        LOGGER.info("Placement rate: 1 order every {} rateMs ms",rateMs);
        LOGGER.info("Pickup time: {} - {} ms after placement", minPickupTimeMs, maxPickupTimeMs);

        // Schedule order placements
        for (int i = 0; i < orders.size(); i++) {
            final int orderIndex = i;
            executor.schedule(
                    () -> placeOrder(orders.get(orderIndex)),
                    (long) i * rateMs,
                    TimeUnit.MILLISECONDS);
        }

        // Wait for all orders to be processed
        while (ordersProcessed.get() < orders.size()) {
            Thread.sleep(100);
        }

        // Shutdown executor and wait for termination
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        LOGGER.info("\nSimulation complete. Processed {} orders.",ordersProcessed.get());

        return actionLogger.getAllActions();
    }

    /**
     * Places an order and schedules its pickup.
     *
     * @param order The order to place
     */
    private void placeOrder(Order order) {
        orderManager.placeOrder(order);
        ordersPlaced.incrementAndGet();

        // Schedule pickup
        int pickupDelay = minPickupTimeMs + random.nextInt(maxPickupTimeMs - minPickupTimeMs + 1);
        executor.schedule(() -> pickupOrder(order.getId()), pickupDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Picks up an order.
     *
     * @param orderId The ID of the order to pick up
     */
    private void pickupOrder(String orderId) {
        orderManager.pickupOrder(orderId);
        ordersProcessed.incrementAndGet();
    }

    /**
     * Gets the options used for this simulation in microseconds, for submission.
     *
     * @return Map of option names to values
     */
    public SubmissionOptions getSubmissionOptions() {
        return new SubmissionOptions(
                rateMs * 1000L, // Convert to microseconds
                minPickupTimeMs * 1000L,
                maxPickupTimeMs * 1000L
        );
    }

    /**
     * Class representing options for challenge submission.
     */
    public static class SubmissionOptions {
        private final long rate;  // Microseconds
        private final long min;   // Microseconds
        private final long max;   // Microseconds

        public SubmissionOptions(long rate, long min, long max) {
            this.rate = rate;
            this.min = min;
            this.max = max;
        }

        public long getRate() {
            return rate;
        }

        public long getMin() {
            return min;
        }

        public long getMax() {
            return max;
        }
    }
}