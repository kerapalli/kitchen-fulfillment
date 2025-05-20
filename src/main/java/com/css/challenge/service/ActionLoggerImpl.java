package com.css.challenge.service;

import com.css.challenge.domain.Action;
import com.css.challenge.domain.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of the ActionLogger interface.
 * Thread-safe to handle concurrent action logging.
 */
public class ActionLoggerImpl implements ActionLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionLoggerImpl.class);
    // Global action log in chronological order
    private final List<Action> actionLog;

    // Order-specific action logs for faster lookup
    private final Map<String, List<Action>> orderActions;

    // Formatter for timestamp display
    private final DateTimeFormatter formatter;

    /**
     * Creates a new action logger.
     */
    public ActionLoggerImpl() {
        this.actionLog = new CopyOnWriteArrayList<>();
        this.orderActions = new ConcurrentHashMap<>();
        this.formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault());
    }

    @Override
    public Action logAction(String orderId, ActionType actionType) {
        return logAction(Instant.now(), orderId, actionType);
    }

    @Override
    public synchronized Action logAction(Instant timestamp, String orderId, ActionType actionType) {
        Action action = new Action(timestamp, orderId, actionType);

        // Add to global log
        actionLog.add(action);

        // Add to order-specific log
        orderActions.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(action);

        // Print the action (for real-time monitoring as specified in requirements)
        LOGGER.info("[{}] Order {}: {}",
                    formatter.format(timestamp),
                    orderId,
                    actionType.name().toLowerCase());

        return action;
    }

    @Override
    public List<Action> getAllActions() {
        return Collections.unmodifiableList(actionLog);
    }

    @Override
    public List<Action> getActionsForOrder(String orderId) {
        return orderActions.getOrDefault(orderId, new ArrayList<>());
    }

    @Override
    public void printActionLog() {
        LOGGER.info("\n===== ACTION LOG =====");

        for (Action action : actionLog) {
            Instant timestamp = Instant.ofEpochMilli(action.getTimestamp() / 1000);
            LOGGER.info("[{}] Order {}: {}",
                    formatter.format(timestamp),
                    action.getId(),
                    action.getActionType());
        }

        LOGGER.info("=====================");
    }

    /**
     * Gets the action log in a format suitable for submission to the challenge server.
     *
     * @return List of Actions
     */
    public List<Action> getSubmissionActions() {
        return new ArrayList<>(actionLog);
    }
}