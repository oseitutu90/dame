package com.dame.entity;

/**
 * Status of an online game session.
 */
public enum OnlineGameStatus {
    /**
     * Waiting for second player to join
     */
    WAITING,

    /**
     * Game is actively being played
     */
    IN_PROGRESS,

    /**
     * Game has ended normally
     */
    COMPLETED,

    /**
     * Game was abandoned (player disconnected/forfeited)
     */
    ABANDONED
}
