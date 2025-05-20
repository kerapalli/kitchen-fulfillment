package com.css.challenge.client;

import com.css.challenge.domain.Action;
import com.css.challenge.domain.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Client for interacting with the challenge server.
 * Handles fetching test problems and submitting solutions.
 */
public class Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    private final String endpoint;
    private final String auth;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new httpClient with the specified endpoint and authentication token.
     *
     * @param endpoint API endpoint URL
     * @param auth Authentication token
     */
    public Client(String endpoint, String auth) {
        this.endpoint = endpoint;
        this.auth = auth;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches a new test problem from the server.
     *
     * @param seed Seed for randomization
     * @return Problem containing test ID and orders
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public Problem newProblem(long seed) throws IOException, InterruptedException {
        // Build URL with query parameters
        StringBuilder urlBuilder = new StringBuilder(endpoint + "/interview/challenge/new?auth=" + auth);

        if (seed != 0) {
            urlBuilder.append("&seed=").append(seed);
        }

        // Create request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlBuilder.toString()))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .GET()
                .build();

        // Send request
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check status
        if (response.statusCode() != 200) {
            throw new IOException(urlBuilder + ": " + response.statusCode() + " " + response.body());
        }

        // Extract test ID from headers
        String id = response.headers().firstValue("x-test-id")
                .orElseThrow(() -> new IOException("Missing test ID in response"));

        LOGGER.info("Fetched new test problem, id={}: {}", id, urlBuilder);

        // Parse orders
        List<Order> orders = Order.parse(response.body());

        return new Problem(id, orders);
    }

    /**
     * Submits a solution to the server.
     *
     * @param testId Test ID
     * @param rate Order placement rate
     * @param min Minimum pickup time
     * @param max Maximum pickup time
     * @param actions List of actions performed
     * @return Server response
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public String solveProblem(
            String testId,
            Duration rate,
            Duration min,
            Duration max,
            List<Action> actions) throws IOException, InterruptedException {

        // Create options object
        ObjectNode optionsNode = objectMapper.createObjectNode();
        optionsNode.put("rate", rate.toMillis() * 1000); // Convert to microseconds
        optionsNode.put("min", min.toMillis() * 1000);   // Convert to microseconds
        optionsNode.put("max", max.toMillis() * 1000);   // Convert to microseconds

        // Create actions array
        ArrayNode actionsNode = objectMapper.createArrayNode();
        for (Action action : actions) {
            ObjectNode actionNode = actionsNode.addObject();
            actionNode.put("timestamp", action.getTimestamp());
            actionNode.put("id", action.getId());
            actionNode.put("action", action.getActionType());
        }

        // Build solution object
        ObjectNode solution = objectMapper.createObjectNode();
        solution.set("options", optionsNode);
        solution.set("actions", actionsNode);

        // Serialize to JSON
        String solutionJson = objectMapper.writeValueAsString(solution);

        // Create request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/interview/challenge/solve?auth=" + auth))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .header("x-test-id", testId)
                .POST(HttpRequest.BodyPublishers.ofString(solutionJson))
                .build();

        // Send request
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check status
        if (response.statusCode() != 200) {
            throw new IOException("Solve problem failed: " + response.statusCode() + " " + response.body());
        }

        return response.body();
    }
}