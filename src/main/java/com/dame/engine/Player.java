package com.dame.engine;

/**
 * Represents the two players in a Dame (Checkers) game.
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Compiled as a special class extending {@code java.lang.Enum}</li>
 *   <li>Each constant (WHITE, BLACK) is a static final singleton instance</li>
 *   <li>Has an implicit ordinal: WHITE=0, BLACK=1 (used in switch statements)</li>
 *   <li>Thread-safe and immutable by design</li>
 * </ul>
 *
 * <h2>Game Rules</h2>
 * <ul>
 *   <li>WHITE always moves first (traditional rule)</li>
 *   <li>WHITE pieces start at rows 5-7 (bottom), move upward (row decreases)</li>
 *   <li>BLACK pieces start at rows 0-2 (top), move downward (row increases)</li>
 * </ul>
 *
 * @see GameLogic#getCurrentPlayer()
 * @see Piece#getOwner()
 */
public enum Player {
    WHITE,
    BLACK;

    /**
     * Returns the opposing player.
     *
     * <p>Used primarily for:</p>
     * <ul>
     *   <li>Switching turns after a move completes</li>
     *   <li>Detecting enemy pieces during capture calculations</li>
     *   <li>Determining winners in forfeit scenarios</li>
     * </ul>
     *
     * @return BLACK if this is WHITE, WHITE if this is BLACK
     */
    public Player opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
