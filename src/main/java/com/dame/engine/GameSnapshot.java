package com.dame.engine;

/**
 * Immutable snapshot of the complete game state for undo functionality.
 * Stores deep copies of the board and all game state variables.
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
