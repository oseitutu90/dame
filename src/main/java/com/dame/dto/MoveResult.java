package com.dame.dto;

import com.dame.engine.GameState;

/**
 * Result of a move attempt in an online game.
 */
public class MoveResult {

    private final boolean success;
    private final boolean turnEnded;
    private final GameState gameState;
    private final String errorMessage;
    private final String multiJumpPositionJson;

    private MoveResult(boolean success, boolean turnEnded, GameState gameState,
                       String errorMessage, String multiJumpPositionJson) {
        this.success = success;
        this.turnEnded = turnEnded;
        this.gameState = gameState;
        this.errorMessage = errorMessage;
        this.multiJumpPositionJson = multiJumpPositionJson;
    }

    public static MoveResult success(boolean turnEnded, GameState gameState, String multiJumpPositionJson) {
        return new MoveResult(true, turnEnded, gameState, null, multiJumpPositionJson);
    }

    public static MoveResult failure(String errorMessage) {
        return new MoveResult(false, false, null, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isTurnEnded() {
        return turnEnded;
    }

    public GameState getGameState() {
        return gameState;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMultiJumpPositionJson() {
        return multiJumpPositionJson;
    }
}
