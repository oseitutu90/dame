package com.dame.engine;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages game state history for undo functionality.
 * Uses a stack (LIFO) to store snapshots before each move.
 *
 * <h2>Undo Flow</h2>
 * <pre>
 *   Before Move:                    After Undo:
 *   ┌─────────────┐                ┌─────────────┐
 *   │  Snapshot N │ ← push()       │  (removed)  │
 *   ├─────────────┤                ├─────────────┤
 *   │  Snapshot 2 │                │  Snapshot 2 │ ← restored
 *   ├─────────────┤                ├─────────────┤
 *   │  Snapshot 1 │                │  Snapshot 1 │
 *   └─────────────┘                └─────────────┘
 * </pre>
 *
 * <h2>Usage Pattern</h2>
 * <pre>
 * // Before executing a move:
 * history.push(GameSnapshot.of(board, player, state, jumpPos));
 *
 * // Execute the move...
 *
 * // To undo:
 * GameSnapshot prev = history.pop();
 * if (prev != null) {
 *     // restore state from prev
 * }
 * </pre>
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Uses {@link ArrayDeque} as stack (faster than Stack class)</li>
 *   <li>No limit on history depth (can undo all moves)</li>
 *   <li>Cleared on game reset via {@link #clear()}</li>
 * </ul>
 *
 * @see GameSnapshot
 * @see GameLogic#undo()
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
