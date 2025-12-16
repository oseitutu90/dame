package com.dame.dto;

/**
 * DTO for online player information in the lobby.
 */
public class OnlinePlayerDTO {

    private final Long id;
    private final String username;
    private final boolean inGame;
    private final boolean inQueue;
    private final int wins;
    private final int losses;

    public OnlinePlayerDTO(Long id, String username, boolean inGame, boolean inQueue,
                          int wins, int losses) {
        this.id = id;
        this.username = username;
        this.inGame = inGame;
        this.inQueue = inQueue;
        this.wins = wins;
        this.losses = losses;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isInGame() {
        return inGame;
    }

    public boolean isInQueue() {
        return inQueue;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public String getStatus() {
        if (inGame) {
            return "In Game";
        } else if (inQueue) {
            return "Finding Match";
        } else {
            return "Available";
        }
    }
}
