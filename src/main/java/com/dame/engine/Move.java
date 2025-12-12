package com.dame.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Move {

    private final Position start;
    private final Position end;
    private final List<Position> captures;

    public Move(Position start, Position end) {
        this.start = start;
        this.end = end;
        this.captures = new ArrayList<>();
    }

    public Move(Position start, Position end, List<Position> captures) {
        this.start = start;
        this.end = end;
        this.captures = new ArrayList<>(captures);
    }

    public Move(int startRow, int startCol, int endRow, int endCol) {
        this(new Position(startRow, startCol), new Position(endRow, endCol));
    }

    public Position getStart() {
        return start;
    }

    public Position getEnd() {
        return end;
    }

    public int getStartRow() {
        return start.row();
    }

    public int getStartCol() {
        return start.col();
    }

    public int getEndRow() {
        return end.row();
    }

    public int getEndCol() {
        return end.col();
    }

    public List<Position> getCaptures() {
        return new ArrayList<>(captures);
    }

    public void addCapture(Position pos) {
        captures.add(pos);
    }

    public void addCapture(int row, int col) {
        captures.add(new Position(row, col));
    }

    public boolean isCapture() {
        return !captures.isEmpty();
    }

    public int getCaptureCount() {
        return captures.size();
    }

    public Move copy() {
        return new Move(start, end, new ArrayList<>(captures));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return Objects.equals(start, move.start) &&
               Objects.equals(end, move.end) &&
               Objects.equals(captures, move.captures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, captures);
    }

    @Override
    public String toString() {
        if (isCapture()) {
            return start + "x" + end + " (captures: " + captures + ")";
        }
        return start + "-" + end;
    }
}
