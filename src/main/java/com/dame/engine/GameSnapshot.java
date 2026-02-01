package com.dame.engine;

/**
 * Immutable snapshot of the complete game state for undo functionality.
 * Stores deep copies of the board and all game state variables.
 *
 * <h2>Captured State</h2>
 * <pre>
 * GameSnapshot {
 *   board            → deep copy of all pieces on the board
 *   currentPlayer    → whose turn it is (WHITE/BLACK)
 *   gameState        → IN_PROGRESS, WHITE_WINS, etc.
 *   multiJumpPosition → position of piece mid-jump, or null
 * }
 * </pre>
 *
 * <h2>Why Deep Copy?</h2>
 * The board is deep-copied to ensure the snapshot remains immutable.
 * Without this, changes to the live board would corrupt historical snapshots.
 *
 * <h2>Usage</h2>
 * <pre>
 * // Before executing move:
 * GameSnapshot snap = GameSnapshot.of(board, player, state, jumpPos);
 * history.push(snap);
 *
 * // After undo:
 * this.board = snap.board().copy(); // copy again to avoid sharing
 * </pre>
 *
 * @param board             deep copy of the board state
 * @param currentPlayer     the player whose turn it is
 * @param gameState         the current game state
 * @param multiJumpPosition the position of a piece mid-jump, or null
 *
 * @see GameHistory
 * @see GameLogic#undo()
 */
public record GameSnapshot(
        Board board,
        Player currentPlayer,
        GameState gameState,
        Position multiJumpPosition) {
    /**
     * Creates a snapshot from the current game state.
     * The board is deep-copied to ensure immutability.
     */
    public static GameSnapshot of(Board board, Player currentPlayer,
            GameState gameState, Position multiJumpPosition) {
        return new GameSnapshot(
                board.copy(),
                currentPlayer,
                gameState,
                multiJumpPosition);
    }
}
