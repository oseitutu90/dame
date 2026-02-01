package com.dame.service;

import com.dame.engine.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UI-scoped service providing game logic access to Vaadin views.
 * Acts as a facade between UI components and the game engine.
 *
 * <h2>Architecture</h2>
 * <pre>
 *   ┌────────────┐     ┌─────────────┐     ┌────────────┐
 *   │ BoardView  │ ──▶ │ DameService │ ──▶ │ GameLogic  │
 *   │ (UI Layer) │     │  (Facade)   │     │  (Engine)  │
 *   └────────────┘     └─────────────┘     └────────────┘
 *                             │
 *                             ▼
 *                      ┌─────────────┐
 *                      │ MatchScore  │
 *                      └─────────────┘
 * </pre>
 *
 * <h2>Scope: @UIScope</h2>
 * Each browser tab/window gets its own instance of DameService.
 * This means:
 * <ul>
 *   <li>Each tab has independent game state</li>
 *   <li>Closing tab destroys the service instance</li>
 *   <li>Multiple users can play separate games simultaneously</li>
 * </ul>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Delegates game logic to {@link GameLogic}</li>
 *   <li>Manages match scoring via {@link MatchScore}</li>
 *   <li>Handles forfeit logic when starting new game mid-match</li>
 *   <li>Provides convenience methods for UI queries</li>
 * </ul>
 *
 * @see BoardView
 * @see GameLogic
 * @see MatchScore
 */
@Service
@UIScope
public class DameService {

    /** The game logic engine for the current game */
    private GameLogic game;

    /** Tracks wins across the best-of-5 match series */
    private final MatchScore matchScore;

    /**
     * Creates a new DameService with fresh game and match state.
     */
    public DameService() {
        this.game = new GameLogic();
        this.matchScore = new MatchScore();
    }

    // ========== GAME CONTROL ==========

    /**
     * Starts a new game within the current match.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>If match is over → does nothing (need resetMatch first)</li>
     *   <li>If current game is over → records the result, starts new game</li>
     *   <li>If current game in progress → counts as forfeit for current player</li>
     * </ul>
     */
    public void newGame() {
        if (!matchScore.isMatchOver()) {
            if (game.isGameOver()) {
                // Record the completed game result
                matchScore.recordGameResult(game.getGameState());
            } else {
                // Game in progress - forfeit for current player
                matchScore.recordForfeit(game.getCurrentPlayer());
            }
        }
        game.reset();
    }

    /**
     * Resets the entire match (scores and game).
     */
    public void resetMatch() {
        matchScore.reset();
        game.reset();
    }

    // ========== MATCH QUERIES ==========

    public MatchScore getMatchScore() {
        return matchScore;
    }

    public boolean isMatchOver() {
        return matchScore.isMatchOver();
    }

    public Player getMatchWinner() {
        return matchScore.getMatchWinner();
    }

    // ========== STATE QUERIES ==========

    public Board getBoard() {
        return game.getBoard();
    }

    public Player getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public GameState getGameState() {
        return game.getGameState();
    }

    public boolean isGameOver() {
        return game.isGameOver();
    }

    public boolean isInMultiJump() {
        return game.isInMultiJump();
    }

    public Position getMultiJumpPosition() {
        return game.getMultiJumpPosition();
    }

    public String getStatusMessage() {
        return game.getStatusMessage();
    }

    // ========== MOVE QUERIES ==========

    public List<Move> getValidMoves() {
        return game.getValidMoves();
    }

    public List<Move> getValidMovesFor(int row, int col) {
        return game.getValidMovesFor(row, col);
    }

    public List<Move> getValidMovesFor(Position pos) {
        return game.getValidMovesFor(pos);
    }

    public boolean canSelect(int row, int col) {
        return game.canSelect(row, col);
    }

    public boolean isOwnPiece(int row, int col) {
        return game.isOwnPiece(row, col);
    }

    // ========== MOVE EXECUTION ==========

    public boolean applyMove(Move move) {
        return game.applyMove(move);
    }

    // ========== UTILITY ==========

    public Piece getPieceAt(int row, int col) {
        return game.getBoard().get(row, col);
    }

    public Piece getPieceAt(Position pos) {
        return game.getBoard().get(pos);
    }

    // ========== UNDO FUNCTIONALITY ==========

    public boolean canUndo() {
        return game.canUndo();
    }

    public boolean undo() {
        return game.undo();
    }
}
