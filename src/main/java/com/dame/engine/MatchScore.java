package com.dame.engine;

/**
 * Tracks match score across a best-of-5 series.
 * First player to reach 3 wins takes the match.
 */
public class MatchScore {

    private static final int TOTAL_GAMES = 5;
    private static final int WINS_NEEDED = 3;

    private int whiteWins;
    private int blackWins;
    private int draws;
    private int gamesPlayed;

    public MatchScore() {
        reset();
    }

    public void reset() {
        whiteWins = 0;
        blackWins = 0;
        draws = 0;
        gamesPlayed = 0;
    }

    /**
     * Records the result of a completed game.
     */
    public void recordGameResult(GameState result) {
        if (isMatchOver()) {
            return; // Don't record after match is complete
        }

        switch (result) {
            case WHITE_WINS -> whiteWins++;
            case BLACK_WINS -> blackWins++;
            case DRAW -> draws++;
            case IN_PROGRESS -> {
                return; // Don't record incomplete games
            }
        }
        gamesPlayed++;
    }

    /**
     * Records a forfeit by the specified player.
     */
    public void recordForfeit(Player forfeitingPlayer) {
        if (isMatchOver()) {
            return;
        }

        if (forfeitingPlayer == Player.WHITE) {
            blackWins++;
        } else {
            whiteWins++;
        }
        gamesPlayed++;
    }

    public boolean isMatchOver() {
        return whiteWins >= WINS_NEEDED || blackWins >= WINS_NEEDED;
    }

    /**
     * Returns the match winner, or null if match is not over.
     */
    public Player getMatchWinner() {
        if (whiteWins >= WINS_NEEDED) {
            return Player.WHITE;
        } else if (blackWins >= WINS_NEEDED) {
            return Player.BLACK;
        }
        return null;
    }

    public int getWinsNeeded() {
        return WINS_NEEDED;
    }

    public int getWhiteWins() {
        return whiteWins;
    }

    public int getBlackWins() {
        return blackWins;
    }

    public int getDraws() {
        return draws;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getCurrentGameNumber() {
        return gamesPlayed + 1;
    }

    public int getTotalGames() {
        return TOTAL_GAMES;
    }

    /**
     * Returns a display string like "White 2 - 1 Black"
     */
    public String getScoreDisplay() {
        return String.format("White %d - %d Black", whiteWins, blackWins);
    }

    /**
     * Returns a display string like "Game 3 - First to 3"
     */
    public String getGameCountDisplay() {
        if (isMatchOver()) {
            return "Match Complete";
        }
        return String.format("Game %d • First to %d", getCurrentGameNumber(), WINS_NEEDED);
    }

    /**
     * Returns the match result message.
     */
    public String getMatchResultMessage() {
        Player winner = getMatchWinner();
        if (winner == null) {
            return "";
        }
        return winner + " wins the match!";
    }
}
