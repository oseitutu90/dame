package com.dame.engine;

import java.util.Objects;

public record Position(int row, int col) {

    /**
     * Checks if this position is within the 8x8 board boundaries.
     *
     * @return true if row and col are both in range [0, 7]
     */
    public boolean isValid() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    /**
     * Checks if this position is a dark (playable) square.
     * In checkers, pieces only exist on dark squares.
     *
     * @return true if (row + col) is odd
     */
    public boolean isDarkSquare() {
        return (row + col) % 2 == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}
