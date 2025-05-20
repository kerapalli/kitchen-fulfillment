package com.css.challenge.service;

import com.css.challenge.domain.Action;
import com.css.challenge.domain.ActionType;

import java.time.Instant;
import java.util.List;

/**
 * Interface for logging kitchen actions.
 */
public interface ActionLogger {

    /**
     * Logs a kitchen action for an order.
     *
     * @param orderId The ID of the order
     * @param actionType The type of action performed
     * @return The created action record
     */
    Action logAction(String orderId, ActionType actionType);

    /**
     * Logs a kitchen action with a specific timestamp.
     *
     * @param timestamp The time when the action occurred
     * @param orderId The ID of the order
     * @param actionType The type of action performed
     * @return The created action record
     */
    Action logAction(Instant timestamp, String orderId, ActionType actionType);

    /**
     * Gets all logged actions.
     *
     * @return List of all actions in chronological order
     */
    List<Action> getAllActions();

    /**
     * Gets actions for a specific order.
     *
     * @param orderId The ID of the order
     * @return List of actions for the order in chronological order
     */
    List<Action> getActionsForOrder(String orderId);

    /**
     * Prints all logged actions to the console in a human-readable format.
     */
    void printActionLog();
}