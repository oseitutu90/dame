package com.dame.engine;

import java.util.List;

public class GameLogic {

    private Board board;
    private Player currentPlayer;
    private MoveCalculator calculator;
    private GameState gameState;
    private Position multiJumpPosition; // Track piece during multi-jump sequence
    private final GameHistory history;

    public GameLogic() {
        this.board = new Board();
        this.board.setupInitialPosition();
        this.currentPlayer = Player.WHITE;
        this.calculator = new MoveCalculator(board);
        this.gameState = GameState.IN_PROGRESS;
        this.multiJumpPosition = null;
        this.history = new GameHistory();
    }

    // ========== GETTERS ==========

    public Board getBoard() {
        return board;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isGameOver() {
        return gameState != GameState.IN_PROGRESS;
    }

    public boolean isInMultiJump() {
        return multiJumpPosition != null;
    }

    public Position getMultiJumpPosition() {
        return multiJumpPosition;
    }

    // ========== MOVE QUERIES ==========

    public List<Move> getValidMoves() {
        if (isGameOver()) {
            return List.of();
        }

        if (isInMultiJump()) {
            return calculator.getCaptureMovesFrom(multiJumpPosition);
        }

        return calculator.getValidMoves(currentPlayer);
    }

    public List<Move> getValidMovesFor(int row, int col) {
        if (isGameOver()) {
            return List.of();
        }

        Piece piece = board.get(row, col);
        if (piece == null || piece.getOwner() != currentPlayer) {
            return List.of();
        }

        if (isInMultiJump()) {
            if (multiJumpPosition.row() != row || multiJumpPosition.col() != col) {
                return List.of(); // Can only move the jumping piece
            }
            return calculator.getCaptureMovesFrom(multiJumpPosition);
        }

        // Check if captures are mandatory
        if (calculator.hasCapturesAvailable(currentPlayer)) {
            return calculator.getMovesForPiece(row, col, piece)
                    .stream()
                    .filter(Move::isCapture)
                    .toList();
        }

        return calculator.getMovesForPiece(row, col, piece);
    }

    public List<Move> getValidMovesFor(Position pos) {
        return getValidMovesFor(pos.row(), pos.col());
    }

    public boolean isOwnPiece(int row, int col) {
        Piece piece = board.get(row, col);
        return piece != null && piece.getOwner() == currentPlayer;
    }

    public boolean canSelect(int row, int col) {
        if (isGameOver()) {
            return false;
        }

        if (isInMultiJump()) {
            return multiJumpPosition.row() == row && multiJumpPosition.col() == col;
        }

        return isOwnPiece(row, col) && !getValidMovesFor(row, col).isEmpty();
    }

    // ========== MOVE EXECUTION ==========

    public boolean applyMove(Move move) {
        if (isGameOver()) {
            return false;
        }

        // Find the actual valid move that matches the requested move
        List<Move> validMoves = isInMultiJump()
                ? calculator.getCaptureMovesFrom(multiJumpPosition)
                : getValidMovesFor(move.getStartRow(), move.getStartCol());

        Move actualMove = validMoves.stream()
                .filter(m -> m.getStart().equals(move.getStart()) && m.getEnd().equals(move.getEnd()))
                .findFirst()
                .orElse(null);

        if (actualMove == null) {
            return false;
        }

        // Save snapshot BEFORE executing the move for undo functionality
        history.push(GameSnapshot.of(board, currentPlayer, gameState, multiJumpPosition));

        // Execute the move using the actual valid move (with correct captures)
        Piece piece = board.get(actualMove.getStart());
        board.movePiece(actualMove.getStart(), actualMove.getEnd());

        // Remove captured pieces from the actual move
        for (Position capture : actualMove.getCaptures()) {
            board.remove(capture);
        }

        // Check for promotion
        checkPromotion(piece, actualMove.getEnd());

        // Check for multi-jump continuation (only for men, kings can choose to stop)
        if (actualMove.isCapture() && !piece.isKing()) {
            List<Move> continuations = calculator.getCaptureMovesFrom(actualMove.getEnd());
            if (!continuations.isEmpty()) {
                multiJumpPosition = actualMove.getEnd();
                return false; // Turn not ended
            }
        }

        // Turn ends
        endTurn();
        return true;
    }

    private void checkPromotion(Piece piece, Position pos) {
        if (piece.isKing()) {
            return;
        }

        // White promotes at row 0, Black promotes at row 7
        if ((piece.getOwner() == Player.WHITE && pos.row() == 0) ||
                (piece.getOwner() == Player.BLACK && pos.row() == 7)) {
            piece.promoteToKing();
        }
    }

    private void endTurn() {
        multiJumpPosition = null;
        currentPlayer = currentPlayer.opponent();
        updateGameState();
    }

    private void updateGameState() {
        // Check if current player has any moves
        if (!calculator.hasValidMoves(currentPlayer)) {
            // Player with no moves loses
            gameState = currentPlayer == Player.WHITE
                    ? GameState.BLACK_WINS
                    : GameState.WHITE_WINS;
            return;
        }

        // Check if either player has no pieces
        int whitePieces = board.countPieces(Player.WHITE);
        int blackPieces = board.countPieces(Player.BLACK);

        if (whitePieces == 0) {
            gameState = GameState.BLACK_WINS;
        } else if (blackPieces == 0) {
            gameState = GameState.WHITE_WINS;
        }
    }

    // ========== GAME CONTROL ==========

    public void reset() {
        this.board = new Board();
        this.board.setupInitialPosition();
        this.currentPlayer = Player.WHITE;
        this.calculator = new MoveCalculator(board);
        this.gameState = GameState.IN_PROGRESS;
        this.multiJumpPosition = null;
        this.history.clear();
    }

    // ========== UNDO FUNCTIONALITY ==========

    /**
     * Checks if undo is available.
     */
    public boolean canUndo() {
        return history.canUndo();
    }

    /**
     * Undoes the last move, restoring the previous game state.
     * 
     * @return true if undo was successful, false if no history available
     */
    public boolean undo() {
        GameSnapshot snapshot = history.pop();
        if (snapshot == null) {
            return false;
        }

        // Restore state from snapshot
        this.board = snapshot.board().copy(); // Copy again to avoid sharing reference
        this.currentPlayer = snapshot.currentPlayer();
        this.gameState = snapshot.gameState();
        this.multiJumpPosition = snapshot.multiJumpPosition();
        this.calculator = new MoveCalculator(board);

        return true;
    }

    public String getStatusMessage() {
        return switch (gameState) {
            case IN_PROGRESS -> {
                if (isInMultiJump()) {
                    yield currentPlayer + " must continue jumping";
                }
                yield currentPlayer + "'s turn";
            }
            case WHITE_WINS -> "White wins!";
            case BLACK_WINS -> "Black wins!";
            case DRAW -> "Draw!";
        };
    }
}
