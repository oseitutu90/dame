package com.dame.dto;

import com.dame.engine.Move;
import com.dame.engine.Position;

import java.util.List;

/**
 * DTO for serializing move information.
 */
public class MoveDTO {

    private final int startRow;
    private final int startCol;
    private final int endRow;
    private final int endCol;
    private final boolean isCapture;
    private final List<int[]> captures;

    public MoveDTO(int startRow, int startCol, int endRow, int endCol,
                   boolean isCapture, List<int[]> captures) {
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
        this.isCapture = isCapture;
        this.captures = captures;
    }

    /**
     * Create from engine Move.
     */
    public static MoveDTO fromMove(Move move) {
        List<int[]> captures = move.getCaptures().stream()
                .map(pos -> new int[]{pos.row(), pos.col()})
                .toList();

        return new MoveDTO(
                move.getStartRow(),
                move.getStartCol(),
                move.getEndRow(),
                move.getEndCol(),
                move.isCapture(),
                captures
        );
    }

    /**
     * Convert to engine Move (for validation, captures won't be set).
     */
    public Move toMove() {
        return new Move(
                new Position(startRow, startCol),
                new Position(endRow, endCol)
        );
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public int getEndCol() {
        return endCol;
    }

    public boolean isCapture() {
        return isCapture;
    }

    public List<int[]> getCaptures() {
        return captures;
    }
}
