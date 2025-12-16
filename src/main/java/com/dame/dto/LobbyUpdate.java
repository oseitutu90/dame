package com.dame.dto;

import java.util.List;

/**
 * DTO for real-time lobby updates broadcast to connected clients.
 */
public class LobbyUpdate {

    public enum UpdateType {
        PLAYER_ONLINE,
        PLAYER_OFFLINE,
        CHALLENGE_RECEIVED,
        CHALLENGE_CANCELLED,
        CHALLENGE_EXPIRED,
        MATCHMAKING_STARTED,
        MATCHMAKING_FOUND,
        MATCHMAKING_CANCELLED,
        GAME_STARTED,
        SPECTATABLE_GAMES_CHANGED,
        FULL_REFRESH
    }

    private final UpdateType type;
    private final Long playerId;
    private final String playerUsername;
    private final Long challengeId;
    private final Long sessionId;
    private final String sessionCode;
    private final List<OnlinePlayerDTO> onlinePlayers;
    private final List<SpectateGameDTO> spectatableGames;
    private final String message;

    private LobbyUpdate(Builder builder) {
        this.type = builder.type;
        this.playerId = builder.playerId;
        this.playerUsername = builder.playerUsername;
        this.challengeId = builder.challengeId;
        this.sessionId = builder.sessionId;
        this.sessionCode = builder.sessionCode;
        this.onlinePlayers = builder.onlinePlayers;
        this.spectatableGames = builder.spectatableGames;
        this.message = builder.message;
    }

    public static Builder builder(UpdateType type) {
        return new Builder(type);
    }

    public UpdateType getType() {
        return type;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public List<OnlinePlayerDTO> getOnlinePlayers() {
        return onlinePlayers;
    }

    public List<SpectateGameDTO> getSpectatableGames() {
        return spectatableGames;
    }

    public String getMessage() {
        return message;
    }

    public static class Builder {
        private final UpdateType type;
        private Long playerId;
        private String playerUsername;
        private Long challengeId;
        private Long sessionId;
        private String sessionCode;
        private List<OnlinePlayerDTO> onlinePlayers;
        private List<SpectateGameDTO> spectatableGames;
        private String message;

        private Builder(UpdateType type) {
            this.type = type;
        }

        public Builder playerId(Long playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder playerUsername(String playerUsername) {
            this.playerUsername = playerUsername;
            return this;
        }

        public Builder challengeId(Long challengeId) {
            this.challengeId = challengeId;
            return this;
        }

        public Builder sessionId(Long sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder sessionCode(String sessionCode) {
            this.sessionCode = sessionCode;
            return this;
        }

        public Builder onlinePlayers(List<OnlinePlayerDTO> onlinePlayers) {
            this.onlinePlayers = onlinePlayers;
            return this;
        }

        public Builder spectatableGames(List<SpectateGameDTO> spectatableGames) {
            this.spectatableGames = spectatableGames;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public LobbyUpdate build() {
            return new LobbyUpdate(this);
        }
    }
}
