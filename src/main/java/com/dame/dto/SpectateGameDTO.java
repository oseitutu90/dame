package com.dame.dto;

import com.dame.entity.OnlineGameSession;

import java.time.LocalDateTime;

/**
 * DTO for games available to spectate.
 */
public class SpectateGameDTO {

    private final Long sessionId;
    private final String sessionCode;
    private final String whitePlayerUsername;
    private final String blackPlayerUsername;
    private final String currentTurn;
    private final int whiteWins;
    private final int blackWins;
    private final int gamesPlayed;
    private final LocalDateTime lastMoveAt;
    private final int spectatorCount;

    public SpectateGameDTO(Long sessionId, String sessionCode,
                          String whitePlayerUsername, String blackPlayerUsername,
                          String currentTurn, int whiteWins, int blackWins, int gamesPlayed,
                          LocalDateTime lastMoveAt, int spectatorCount) {
        this.sessionId = sessionId;
        this.sessionCode = sessionCode;
        this.whitePlayerUsername = whitePlayerUsername;
        this.blackPlayerUsername = blackPlayerUsername;
        this.currentTurn = currentTurn;
        this.whiteWins = whiteWins;
        this.blackWins = blackWins;
        this.gamesPlayed = gamesPlayed;
        this.lastMoveAt = lastMoveAt;
        this.spectatorCount = spectatorCount;
    }

    public static SpectateGameDTO fromSession(OnlineGameSession session, int spectatorCount) {
        return new SpectateGameDTO(
                session.getId(),
                session.getSessionCode(),
                session.getWhitePlayer() != null ? session.getWhitePlayer().getUsername() : "?",
                session.getBlackPlayer() != null ? session.getBlackPlayer().getUsername() : "?",
                session.getCurrentTurn(),
                session.getWhiteWins(),
                session.getBlackWins(),
                session.getGamesPlayed(),
                session.getLastMoveAt(),
                spectatorCount
        );
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public String getWhitePlayerUsername() {
        return whitePlayerUsername;
    }

    public String getBlackPlayerUsername() {
        return blackPlayerUsername;
    }

    public String getCurrentTurn() {
        return currentTurn;
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

    public LocalDateTime getLastMoveAt() {
        return lastMoveAt;
    }

    public int getSpectatorCount() {
        return spectatorCount;
    }

    public String getMatchupDisplay() {
        return whitePlayerUsername + " vs " + blackPlayerUsername;
    }

    public String getScoreDisplay() {
        return whiteWins + " - " + blackWins;
    }
}
