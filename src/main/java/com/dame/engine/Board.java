package com.dame.engine;

/**
 * Represents the 8x8 game board holding all pieces.
 *
 * <h2>Board Layout</h2>
 * <pre>
 *      col→  0   1   2   3   4   5   6   7
 *    row ┌───┬───┬───┬───┬───┬───┬───┬───┐
 *     0  │ L │ D │ L │ D │ L │ D │ L │ D │  ← BLACK's back row (promotion zone for WHITE)
 *     1  │ D │ L │ D │ L │ D │ L │ D │ L │
 *     2  │ L │ D │ L │ D │ L │ D │ L │ D │
 *     3  │ D │ L │ D │ L │ D │ L │ D │ L │  ← Empty at start
 *     4  │ L │ D │ L │ D │ L │ D │ L │ D │  ← Empty at start
 *     5  │ D │ L │ D │ L │ D │ L │ D │ L │
 *     6  │ L │ D │ L │ D │ L │ D │ L │ D │
 *     7  │ D │ L │ D │ L │ D │ L │ D │ L │  ← WHITE's back row (promotion zone for BLACK)
 *        └───┴───┴───┴───┴───┴───┴───┴───┘
 *    L = Light square (never used)
 *    D = Dark square (playable)
 * </pre>
 *
 * <h2>Initial Setup</h2>
 * <ul>
 *   <li>BLACK pieces: rows 0-2, dark squares only (12 pieces)</li>
 *   <li>WHITE pieces: rows 5-7, dark squares only (12 pieces)</li>
 *   <li>Rows 3-4: empty (the "no man's land")</li>
 * </ul>
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Internally uses a {@code Piece[8][8]} 2D array</li>
 *   <li>null means empty square</li>
 *   <li>{@link #copy()} creates a deep copy (clones all Piece objects)</li>
 *   <li>Serialized to JSON by {@link BoardStateSerializer} for persistence</li>
 * </ul>
 *
 * @see Piece
 * @see GameLogic
 * @see MoveCalculator
 */
public class Board {

    /** Standard board size (8x8 squares) */
    public static final int SIZE = 8;

    /** The 2D array holding pieces. null = empty square */
    private final Piece[][] grid;

    /**
     * Creates an empty board (no pieces placed).
     * Call {@link #setupInitialPosition()} to place starting pieces.
     */
    public Board() {
        this.grid = new Piece[SIZE][SIZE];
    }

    public Piece get(int row, int col) {
        if (!isInside(row, col)) {
            return null;
        }
        return grid[row][col];
    }

    public Piece get(Position pos) {
        return get(pos.row(), pos.col());
    }

    public void set(int row, int col, Piece piece) {
        if (isInside(row, col)) {
            grid[row][col] = piece;
        }
    }

    public void set(Position pos, Piece piece) {
        set(pos.row(), pos.col(), piece);
    }

    public void remove(int row, int col) {
        set(row, col, null);
    }

    public void remove(Position pos) {
        remove(pos.row(), pos.col());
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public boolean isInside(Position pos) {
        return isInside(pos.row(), pos.col());
    }

    public boolean isEmpty(int row, int col) {
        return get(row, col) == null;
    }

    public boolean isEmpty(Position pos) {
        return isEmpty(pos.row(), pos.col());
    }

    public boolean isDarkSquare(int row, int col) {
        return (row + col) % 2 == 1;
    }

    public void movePiece(Position from, Position to) {
        Piece piece = get(from);
        remove(from);
        set(to, piece);
    }

    public int countPieces(Player player) {
        int count = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Piece p = get(r, c);
                if (p != null && p.getOwner() == player) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Count the number of kings for a player.
     */
    public int countKings(Player player) {
        int count = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Piece p = get(r, c);
                if (p != null && p.getOwner() == player && p.isKing()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Count the number of men (non-king pieces) for a player.
     */
    public int countMen(Player player) {
        return countPieces(player) - countKings(player);
    }

    public Board copy() {
        Board copy = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Piece p = grid[r][c];
                if (p != null) {
                    copy.grid[r][c] = p.copy();
                }
            }
        }
        return copy;
    }

    public void setupInitialPosition() {
        // Clear the board
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = null;
            }
        }

        // Place BLACK pieces (rows 0-2)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isDarkSquare(r, c)) {
                    grid[r][c] = new Piece(Player.BLACK);
                }
            }
        }

        // Place WHITE pieces (rows 5-7)
        for (int r = 5; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isDarkSquare(r, c)) {
                    grid[r][c] = new Piece(Player.WHITE);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  0 1 2 3 4 5 6 7\n");
        for (int r = 0; r < SIZE; r++) {
            sb.append(r).append(" ");
            for (int c = 0; c < SIZE; c++) {
                Piece p = grid[r][c];
                if (p == null) {
                    sb.append(isDarkSquare(r, c) ? "." : " ");
                } else if (p.getOwner() == Player.WHITE) {
                    sb.append(p.isKing() ? "W" : "w");
                } else {
                    sb.append(p.isKing() ? "B" : "b");
                }
                sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
