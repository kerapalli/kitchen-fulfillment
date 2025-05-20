package com.css.challenge;

import com.css.challenge.domain.Action;
import com.css.challenge.exception.InvalidOrderException;
import com.css.challenge.service.ActionLogger;
import com.css.challenge.service.ActionLoggerImpl;
import com.css.challenge.service.FreshnessTracker;
import com.css.challenge.service.FreshnessTrackerImpl;
import com.css.challenge.service.OrderManager;
import com.css.challenge.service.OrderManagerImpl;
import com.css.challenge.storage.Kitchen;
import com.css.challenge.strategy.CompositeDiscardStrategy;
import com.css.challenge.strategy.DiscardStrategy;
import com.css.challenge.strategy.FreshnessDiscardStrategy;
import com.css.challenge.strategy.TemperatureMismatchDiscardStrategy;
import com.css.challenge.client.Client;
import com.css.challenge.client.Problem;
import com.css.challenge.client.Simulator;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main entry point for the kitchen order fulfillment system.
 * Handles command-line arguments, initializes components, and runs the simulation.
 */
@Command(name = "kitchen-fulfillment",
        description = "Food order fulfillment system for delivery-only kitchen",
        mixinStandardHelpOptions = true,
        version = "1.0.0")
public class Main implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-e", "--endpoint"}, description = "API endpoint URL")
    private String endpoint = "https://api.cloudkitchens.com";

    @Option(names = {"-a", "--auth"}, description = "Authentication token (required)", required = true)
    private String auth;

    @Option(names = {"-s", "--seed"}, description = "Random seed (0 for random)")
    private long seed = 0;

    @Option(names = {"-r", "--rate"}, description = "Order rate in milliseconds")
    private int rateMs = 500;

    @Option(names = {"--min"}, description = "Minimum pickup time in milliseconds")
    private int minPickupMs = 4000;

    @Option(names = {"--max"}, description = "Maximum pickup time in milliseconds")
    private int maxPickupMs = 8000;

    @Option(names = {"-d", "--discard-strategy"}, description = "Discard strategy: freshness, temperature, composite")
    private String discardStrategyName = "composite";

    @Option(names = {"--heater-capacity"}, description = "Heater capacity")
    private int heaterCapacity = 6;

    @Option(names = {"--cooler-capacity"}, description = "Cooler capacity")
    private int coolerCapacity = 6;

    @Option(names = {"--shelf-capacity"}, description = "Shelf capacity")
    private int shelfCapacity = 12;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose = false;

    /**
     * Main entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Executes the application logic.
     *
     * @return Exit code (0 for success, non-zero for error)
     */
    @Override
    public Integer call() {
        try {
            // Initialize components
            Kitchen kitchen = new Kitchen(heaterCapacity, coolerCapacity, shelfCapacity);
            ActionLogger actionLogger = new ActionLoggerImpl();
            FreshnessTracker freshnessTracker = new FreshnessTrackerImpl();
            DiscardStrategy discardStrategy = createDiscardStrategy();

            OrderManager orderManager = new OrderManagerImpl(
                    kitchen,
                    actionLogger,
                    freshnessTracker,
                    discardStrategy);

            // Validate parameters
            validateParameters();

            // Print configuration
            if (verbose) {
                printConfiguration(discardStrategy);
            }

            // Fetch problem from server
            Client client = new Client(endpoint, auth);
            Problem problem = client.newProblem(seed);

            // Log problem details
            LOGGER.info("Received test problem: id={}, orders={}", problem.getTestId(), problem.getOrderCount());
            LOGGER.info("Received test problem with {} orders (ID: {})",
                    problem.getOrderCount(), problem.getTestId());

            // Run simulation
            Simulator simulator = new Simulator(
                    problem.getOrders(),
                    orderManager,
                    actionLogger,
                    rateMs,
                    minPickupMs,
                    maxPickupMs);

            List<Action> actions = simulator.run();

            // Submit solution
            String result = client.solveProblem(
                    problem.getTestId(),
                    Duration.ofMillis(rateMs),
                    Duration.ofMillis(minPickupMs),
                    Duration.ofMillis(maxPickupMs),
                    actions);

            // log result
            LOGGER.info("\nResult: {}", result);

            return 0; // Success

        } catch (Exception e) {
            LOGGER.error("Error running simulation", e);
            if (verbose) {
                e.printStackTrace();
            }

            return 1; // Error
        }
    }

    /**
     * Creates a discard strategy based on the specified strategy name.
     *
     * @return The configured discard strategy
     */
    private DiscardStrategy createDiscardStrategy() {
        return switch (discardStrategyName.toLowerCase()) {
            case "freshness" -> new FreshnessDiscardStrategy();
            case "temperature" -> new TemperatureMismatchDiscardStrategy();
            default -> new CompositeDiscardStrategy();
        };
    }

    /**
     * Validates command-line parameters.
     *
     * @throws InvalidOrderException If parameters are invalid
     */
    private void validateParameters() {
        if (rateMs <= 0) {
            throw new InvalidOrderException("Rate must be greater than zero");
        }

        if (minPickupMs <= 0) {
            throw new InvalidOrderException("Minimum pickup time must be greater than zero");
        }

        if (maxPickupMs <= 0) {
            throw new InvalidOrderException("Maximum pickup time must be greater than zero");
        }

        if (minPickupMs >= maxPickupMs) {
            throw new InvalidOrderException("Minimum pickup time must be less than maximum pickup time");
        }

        if (heaterCapacity <= 0 || coolerCapacity <= 0 || shelfCapacity <= 0) {
            throw new InvalidOrderException("Storage capacities must be greater than zero");
        }
    }

    /**
     * Prints the current configuration.
     *
     * @param discardStrategy The configured discard strategy
     */
    private void printConfiguration(DiscardStrategy discardStrategy) {
        LOGGER.info("Configuration:");
        LOGGER.info("  - Endpoint: {}", endpoint);
        LOGGER.info("  - Seed: {}", (seed == 0 ? "random" : seed));
        LOGGER.info("  - Rate: {} ms", rateMs);
        LOGGER.info("  - Pickup time: {} - {} ms",minPickupMs,maxPickupMs);
        LOGGER.info("  - Discard strategy: {}", discardStrategy.getClass().getSimpleName());
        LOGGER.info("  - Storage capacities:");
        LOGGER.info("    * Heater: {}", heaterCapacity);
        LOGGER.info("    * Cooler: {}" ,coolerCapacity);
        LOGGER.info("    * Shelf: {}", shelfCapacity);
    }
}