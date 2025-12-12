package com.dame.engine;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages game state history for undo functionality.
 * Uses a stack (LIFO) to store snapshots before each move.
 */
public class GameHistory {

    private final Deque<GameSnapshot> history;

    public GameHistory() {
        this.history = new ArrayDeque<>();
    }

    /**
     * Saves a game state snapshot to the history.
     * Call this BEFORE executing a move.
     */
    public void push(GameSnapshot snapshot) {
        history.push(snapshot);
    }

    /**
     * Retrieves and removes the most recent snapshot.
     * 
     * @return the previous game state, or null if history is empty
     */
    public GameSnapshot pop() {
        return history.isEmpty() ? null : history.pop();
    }

    /**
     * Checks if undo is available.
     * 
     * @return true if there's at least one snapshot in history
     */
    public boolean canUndo() {
        return !history.isEmpty();
    }

    /**
     * Clears all history. Call this when starting a new game.
     */
    public void clear() {
        history.clear();
    }

    /**
     * Returns the number of snapshots in history.
     */
    public int size() {
        return history.size();
    }
}
