package com.dame.engine;

import java.util.ArrayList;
import java.util.List;

public class MoveCalculator {

    private final Board board;

    // Direction vectors for diagonal movement
    private static final int[][] MAN_DIRECTIONS_WHITE = { { -1, -1 }, { -1, 1 } }; // White moves up
    private static final int[][] MAN_DIRECTIONS_BLACK = { { 1, -1 }, { 1, 1 } }; // Black moves down
    private static final int[][] ALL_DIRECTIONS = { { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 } };

    public MoveCalculator(Board board) {
        this.board = board;
    }

    public List<Move> getValidMoves(Player player) {
        List<Move> allMoves = new ArrayList<>();
        List<Move> captureMoves = new ArrayList<>();

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece piece = board.get(r, c);
                if (piece == null || piece.getOwner() != player) {
                    continue;
                }

                List<Move> pieceMoves = getMovesForPiece(r, c, piece);
                for (Move move : pieceMoves) {
                    if (move.isCapture()) {
                        captureMoves.add(move);
                    } else {
                        allMoves.add(move);
                    }
                }
            }
        }

        // Ghanaian rule: captures are mandatory
        return captureMoves.isEmpty() ? allMoves : captureMoves;
    }

    public List<Move> getMovesForPiece(int row, int col, Piece piece) {
        if (piece == null) {
            return new ArrayList<>();
        }

        List<Move> moves = new ArrayList<>();

        if (piece.isKing()) {
            moves.addAll(getKingCaptures(row, col, piece));
            if (moves.isEmpty()) {
                moves.addAll(getKingSimpleMoves(row, col));
            }
        } else {
            moves.addAll(getManCaptures(row, col, piece));
            if (moves.isEmpty()) {
                moves.addAll(getManSimpleMoves(row, col, piece));
            }
        }

        return moves;
    }

    public List<Move> getMovesForPosition(Position pos) {
        Piece piece = board.get(pos);
        if (piece == null) {
            return new ArrayList<>();
        }
        return getMovesForPiece(pos.row(), pos.col(), piece);
    }

    // ========== MAN SIMPLE MOVES ==========

    private List<Move> getManSimpleMoves(int row, int col, Piece piece) {
        List<Move> moves = new ArrayList<>();
        int[][] directions = piece.getOwner() == Player.WHITE
                ? MAN_DIRECTIONS_WHITE
                : MAN_DIRECTIONS_BLACK;

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (board.isInside(newRow, newCol) && board.isEmpty(newRow, newCol)) {
                moves.add(new Move(row, col, newRow, newCol));
            }
        }

        return moves;
    }

    // ========== MAN CAPTURES ==========
    // Ghanaian rule: Men can capture backward

    private List<Move> getManCaptures(int row, int col, Piece piece) {
        List<Move> captures = new ArrayList<>();
        findManCaptureSequences(row, col, piece, new ArrayList<>(), captures, board.copy());
        return captures;
    }

    private void findManCaptureSequences(int row, int col, Piece piece,
            List<Position> capturedSoFar, List<Move> allCaptures, Board boardState) {

        boolean foundCapture = false;

        // Men can capture in all four diagonal directions (Ghanaian rule)
        for (int[] dir : ALL_DIRECTIONS) {
            int midRow = row + dir[0];
            int midCol = col + dir[1];
            int endRow = row + 2 * dir[0];
            int endCol = col + 2 * dir[1];

            if (!boardState.isInside(endRow, endCol)) {
                continue;
            }

            Piece midPiece = boardState.get(midRow, midCol);
            Position midPos = new Position(midRow, midCol);

            // Check if there's an enemy piece to capture and landing square is empty
            if (midPiece != null &&
                    midPiece.getOwner() != piece.getOwner() &&
                    !capturedSoFar.contains(midPos) &&
                    boardState.isEmpty(endRow, endCol)) {

                foundCapture = true;

                // Make the capture on a copy of the board
                Board nextBoard = boardState.copy();
                nextBoard.remove(midRow, midCol);
                nextBoard.movePiece(new Position(row, col), new Position(endRow, endCol));

                List<Position> newCaptured = new ArrayList<>(capturedSoFar);
                newCaptured.add(midPos);

                // Recursively look for more captures (multi-jump)
                findManCaptureSequences(endRow, endCol, piece, newCaptured, allCaptures, nextBoard);
            }
        }

        // If no more captures found and we have captured at least one piece, record the
        // move
        if (!foundCapture && !capturedSoFar.isEmpty()) {
            Position start = new Position(
                    row - sumDirections(capturedSoFar, 0),
                    col - sumDirections(capturedSoFar, 1));
            // Recalculate start position from captures
            Move move = new Move(calculateStartPosition(row, col, capturedSoFar),
                    new Position(row, col), capturedSoFar);
            allCaptures.add(move);
        }
    }

    private Position calculateStartPosition(int endRow, int endCol, List<Position> captures) {
        // Work backwards from the end position through all captures
        int row = endRow;
        int col = endCol;

        for (int i = captures.size() - 1; i >= 0; i--) {
            Position cap = captures.get(i);
            // The piece jumped over this capture, so go back
            int dRow = cap.row() - row;
            int dCol = cap.col() - col;
            // Normalize direction
            dRow = Integer.signum(dRow);
            dCol = Integer.signum(dCol);
            // Go back one more step (start was one step before the captured piece)
            row = cap.row() + dRow;
            col = cap.col() + dCol;
        }

        return new Position(row, col);
    }

    private int sumDirections(List<Position> positions, int index) {
        // Helper method - not actually used in final calculation
        return 0;
    }

    // ========== KING SIMPLE MOVES ==========
    // Ghanaian rule: Flying kings - can move any distance diagonally

    private List<Move> getKingSimpleMoves(int row, int col) {
        List<Move> moves = new ArrayList<>();

        for (int[] dir : ALL_DIRECTIONS) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // Flying king: continue in direction until blocked or edge
            while (board.isInside(newRow, newCol) && board.isEmpty(newRow, newCol)) {
                moves.add(new Move(row, col, newRow, newCol));
                newRow += dir[0];
                newCol += dir[1];
            }
        }

        return moves;
    }

    // ========== KING CAPTURES ==========
    // Ghanaian rule: Flying kings can capture from distance

    private List<Move> getKingCaptures(int row, int col, Piece piece) {
        List<Move> captures = new ArrayList<>();
        Position originalStart = new Position(row, col);
        findKingCaptureSequences(row, col, piece, originalStart, new ArrayList<>(), captures, board.copy());
        return captures;
    }

    private void findKingCaptureSequences(int row, int col, Piece piece, Position originalStart,
            List<Position> capturedSoFar, List<Move> allCaptures, Board boardState) {

        boolean foundCapture = false;

        for (int[] dir : ALL_DIRECTIONS) {
            int scanRow = row + dir[0];
            int scanCol = col + dir[1];

            // Scan along diagonal until we hit something or edge
            while (boardState.isInside(scanRow, scanCol)) {
                Piece scannedPiece = boardState.get(scanRow, scanCol);

                if (scannedPiece != null) {
                    Position enemyPos = new Position(scanRow, scanCol);

                    // Found an enemy piece that hasn't been captured yet
                    if (scannedPiece.getOwner() != piece.getOwner() &&
                            !capturedSoFar.contains(enemyPos)) {

                        // Check landing squares beyond the enemy
                        int landRow = scanRow + dir[0];
                        int landCol = scanCol + dir[1];

                        while (boardState.isInside(landRow, landCol) &&
                                boardState.isEmpty(landRow, landCol)) {

                            foundCapture = true;

                            Board nextBoard = boardState.copy();
                            nextBoard.remove(scanRow, scanCol);
                            nextBoard.movePiece(new Position(row, col), new Position(landRow, landCol));

                            List<Position> newCaptured = new ArrayList<>(capturedSoFar);
                            newCaptured.add(enemyPos);

                            findKingCaptureSequences(landRow, landCol, piece, originalStart,
                                    newCaptured, allCaptures, nextBoard);

                            landRow += dir[0];
                            landCol += dir[1];
                        }
                    }
                    // Stop scanning in this direction (blocked by any piece)
                    break;
                }

                scanRow += dir[0];
                scanCol += dir[1];
            }
        }

        if (!foundCapture && !capturedSoFar.isEmpty()) {
            Move move = new Move(originalStart, new Position(row, col), capturedSoFar);
            allCaptures.add(move);
        }
    }

    // calculateKingStartPosition removed - now passing originalStart through
    // recursion

    // ========== UTILITY METHODS ==========

    public boolean hasValidMoves(Player player) {
        return !getValidMoves(player).isEmpty();
    }

    public boolean hasCapturesAvailable(Player player) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece piece = board.get(r, c);
                if (piece != null && piece.getOwner() == player) {
                    List<Move> moves = getMovesForPiece(r, c, piece);
                    if (moves.stream().anyMatch(Move::isCapture)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Move> getCaptureMovesFrom(Position pos) {
        Piece piece = board.get(pos);
        if (piece == null) {
            return new ArrayList<>();
        }
        return getMovesForPiece(pos.row(), pos.col(), piece)
                .stream()
                .filter(Move::isCapture)
                .toList();
    }
}
