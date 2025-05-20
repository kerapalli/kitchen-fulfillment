package com.css.challenge.client;

import com.css.challenge.domain.Order;
import lombok.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Represents a test problem from the challenge server.
 * Contains a test ID and a list of orders to process.
 */
public class Problem {
    @NonNull
    private final String testId;
    @NonNull
    private final List<Order> orders;

    /**
     * Creates a new problem.
     *
     * @param testId Unique identifier for the test
     * @param orders List of orders to process
     */
    public Problem(String testId, List<Order> orders) {
        this.testId = testId;
        this.orders = orders;
    }

    /**
     * Gets the test ID.
     *
     * @return Test ID
     */
    public String getTestId() {
        return testId;
    }

    /**
     * Gets the list of orders.
     *
     * @return List of orders
     */
    public List<Order> getOrders() {
        return orders;
    }

    /**
     * Gets the number of orders in this problem.
     *
     * @return Number of orders
     */
    public int getOrderCount() {
        return orders.size();
    }

    @Override
    public String toString() {
        return "Problem{" +
                "testId='" + testId + '\'' +
                ", orders=" + orders.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Problem problem = (Problem) o;
        return Objects.equals(testId, problem.testId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testId);
    }
}