package com.dame.dto;

import com.dame.engine.GameState;
import com.dame.entity.OnlineGameStatus;

/**
 * DTO for real-time game state updates broadcast to connected clients.
 */
public class GameUpdate {

    public enum UpdateType {
        MOVE_MADE,
        GAME_STARTED,
        GAME_ENDED,
        PLAYER_CONNECTED,
        PLAYER_DISCONNECTED,
        PLAYER_FORFEITED,
        NEW_ROUND,
        SESSION_UPDATED
    }

    private final UpdateType type;
    private final Long sessionId;
    private final String boardStateJson;
    private final String currentTurn;
    private final GameState gameState;
    private final OnlineGameStatus sessionStatus;
    private final String multiJumpPositionJson;
    private final int whiteWins;
    private final int blackWins;
    private final int gamesPlayed;
    private final String message;
    private final MoveDTO lastMove;

    private GameUpdate(Builder builder) {
        this.type = builder.type;
        this.sessionId = builder.sessionId;
        this.boardStateJson = builder.boardStateJson;
        this.currentTurn = builder.currentTurn;
        this.gameState = builder.gameState;
        this.sessionStatus = builder.sessionStatus;
        this.multiJumpPositionJson = builder.multiJumpPositionJson;
        this.whiteWins = builder.whiteWins;
        this.blackWins = builder.blackWins;
        this.gamesPlayed = builder.gamesPlayed;
        this.message = builder.message;
        this.lastMove = builder.lastMove;
    }

    public static Builder builder(UpdateType type, Long sessionId) {
        return new Builder(type, sessionId);
    }

    public UpdateType getType() {
        return type;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getBoardStateJson() {
        return boardStateJson;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public GameState getGameState() {
        return gameState;
    }

    public OnlineGameStatus getSessionStatus() {
        return sessionStatus;
    }

    public String getMultiJumpPositionJson() {
        return multiJumpPositionJson;
    }

    public int getWhiteWins() {
        return whiteWins;
    }

    public int getBlackWins() {
        return blackWins;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public String getMessage() {
        return message;
    }

    public MoveDTO getLastMove() {
        return lastMove;
    }

    public static class Builder {
        private final UpdateType type;
        private final Long sessionId;
        private String boardStateJson;
        private String currentTurn;
        private GameState gameState;
        private OnlineGameStatus sessionStatus;
        private String multiJumpPositionJson;
        private int whiteWins;
        private int blackWins;
        private int gamesPlayed;
        private String message;
        private MoveDTO lastMove;

        private Builder(UpdateType type, Long sessionId) {
            this.type = type;
            this.sessionId = sessionId;
        }

        public Builder boardStateJson(String boardStateJson) {
            this.boardStateJson = boardStateJson;
            return this;
        }

        public Builder currentTurn(String currentTurn) {
            this.currentTurn = currentTurn;
            return this;
        }

        public Builder gameState(GameState gameState) {
            this.gameState = gameState;
            return this;
        }

        public Builder sessionStatus(OnlineGameStatus sessionStatus) {
            this.sessionStatus = sessionStatus;
            return this;
        }

        public Builder multiJumpPositionJson(String multiJumpPositionJson) {
            this.multiJumpPositionJson = multiJumpPositionJson;
            return this;
        }

        public Builder whiteWins(int whiteWins) {
            this.whiteWins = whiteWins;
            return this;
        }

        public Builder blackWins(int blackWins) {
            this.blackWins = blackWins;
            return this;
        }

        public Builder gamesPlayed(int gamesPlayed) {
            this.gamesPlayed = gamesPlayed;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder lastMove(MoveDTO lastMove) {
            this.lastMove = lastMove;
            return this;
        }

        public GameUpdate build() {
            return new GameUpdate(this);
        }
    }
}
