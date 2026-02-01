package com.dame.engine;

/**
 * Tracks match score across a best-of-5 series.
 * First player to reach 3 wins takes the match.
 *
 * <h2>Match Structure</h2>
 * <pre>
 *   Game 1 → Game 2 → Game 3 → Game 4 → Game 5
 *      │        │        │        │        │
 *      ▼        ▼        ▼        ▼        ▼
 *   Record  Record   Record   Record   Record
 *   Result  Result   Result   Result   Result
 *      │        │        │        │        │
 *      └────────┴────────┴────────┴────────┘
 *                       ▼
 *              First to 3 wins!
 * </pre>
 *
 * <h2>Game Results</h2>
 * <ul>
 *   <li>{@link GameState#WHITE_WINS} → whiteWins++</li>
 *   <li>{@link GameState#BLACK_WINS} → blackWins++</li>
 *   <li>{@link GameState#DRAW} → draws++ (no winner credit)</li>
 *   <li>{@link GameState#IN_PROGRESS} → ignored (game not complete)</li>
 * </ul>
 *
 * <h2>Forfeit Rules</h2>
 * If a player forfeits (e.g., clicks "New Game" during an active game),
 * the opponent receives a win credit.
 *
 * <h2>Match End Conditions</h2>
 * The match ends when either player reaches {@code WINS_NEEDED} (3).
 * Note: Draws count as games played but don't contribute to wins.
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Thread-safety: NOT thread-safe (single-threaded UI expected)</li>
 *   <li>TOTAL_GAMES (5) is tracked but not enforced as end condition</li>
 *   <li>Match winner is purely based on first-to-3-wins</li>
 * </ul>
 *
 * @see DameService
 * @see GameState
 */
public class MatchScore {

    /** Maximum games in a match (for display purposes) */
    private static final int TOTAL_GAMES = 5;

    /** Number of wins required to win the match */
    private static final int WINS_NEEDED = 3;

    /** Number of games won by WHITE */
    private int whiteWins;

    /** Number of games won by BLACK */
    private int blackWins;

    /** Number of drawn games (neither player wins credit) */
    private int draws;

    /** Total games completed (wins + draws) */
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
