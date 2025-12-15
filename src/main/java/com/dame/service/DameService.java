package com.dame.service;

import com.dame.engine.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@UIScope
public class DameService {

    private GameLogic game;
    private final MatchScore matchScore;

    public DameService() {
        this.game = new GameLogic();
        this.matchScore = new MatchScore();
    }

    // ========== GAME CONTROL ==========

    /**
     * Starts a new game within the current match.
     * If a game is in progress, it counts as a forfeit for the current player.
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
