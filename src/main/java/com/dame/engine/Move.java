package com.dame.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a move from one position to another, optionally with captures.
 *
 * <h2>Move Types</h2>
 * <table border="1">
 *   <tr><th>Type</th><th>Description</th><th>captures list</th></tr>
 *   <tr>
 *     <td>Simple Move</td>
 *     <td>Moving to an adjacent empty square</td>
 *     <td>Empty</td>
 *   </tr>
 *   <tr>
 *     <td>Single Capture</td>
 *     <td>Jumping over one enemy piece</td>
 *     <td>1 position</td>
 *   </tr>
 *   <tr>
 *     <td>Multi-Jump</td>
 *     <td>Chain of captures in one turn</td>
 *     <td>2+ positions</td>
 *   </tr>
 * </table>
 *
 * <h2>Ghanaian Dame Rules</h2>
 * <ul>
 *   <li><b>Mandatory capture:</b> If any capture is available, player MUST capture</li>
 *   <li><b>Free choice:</b> Player may choose ANY capture sequence (not required to maximize)</li>
 *   <li><b>Multi-jump:</b> If more captures available after landing, must continue</li>
 * </ul>
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Start/end positions are immutable after construction</li>
 *   <li>Captures list can be built incrementally during move calculation</li>
 *   <li>Used for equality checks when validating player-requested moves</li>
 *   <li>Transferred as {@link com.dame.dto.MoveDTO} for online games</li>
 * </ul>
 *
 * @see MoveCalculator
 * @see GameLogic#applyMove(Move)
 */
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

    /**
     * @return defensive copy of captured positions
     */
    public List<Position> getCaptures() {
        return new ArrayList<>(captures);
    }

    /**
     * Adds a captured piece position (used during move calculation).
     */
    public void addCapture(Position pos) {
        captures.add(pos);
    }

    /**
     * Adds a captured piece position by coordinates.
     */
    public void addCapture(int row, int col) {
        captures.add(new Position(row, col));
    }

    /**
     * @return true if this move captures at least one piece
     */
    public boolean isCapture() {
        return !captures.isEmpty();
    }

    /**
     * @return number of pieces captured in this move
     */
    public int getCaptureCount() {
        return captures.size();
    }

    /**
     * Creates a deep copy of this move.
     */
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
