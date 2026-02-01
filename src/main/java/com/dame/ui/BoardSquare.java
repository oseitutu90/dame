package com.dame.ui;

import com.dame.engine.Piece;
import com.dame.engine.PieceType;
import com.dame.engine.Player;
import com.vaadin.flow.component.html.Div;

/**
 * Represents a single square on the game board.
 * Handles piece display and interaction states (selected, highlighted).
 *
 * <h2>Visual States</h2>
 * <table border="1">
 *   <tr><th>CSS Class</th><th>Meaning</th></tr>
 *   <tr><td>light-square</td><td>Non-playable square (pieces never here)</td></tr>
 *   <tr><td>dark-square</td><td>Playable square (checkers pattern)</td></tr>
 *   <tr><td>white-piece</td><td>Contains a white piece</td></tr>
 *   <tr><td>black-piece</td><td>Contains a black piece</td></tr>
 *   <tr><td>king-piece</td><td>Piece is a king (adds crown indicator)</td></tr>
 *   <tr><td>selected</td><td>Currently selected piece</td></tr>
 *   <tr><td>highlighted</td><td>Valid move destination</td></tr>
 * </table>
 *
 * <h2>Coordinate System</h2>
 * <pre>
 *   row 0 = top of board (BLACK's back row)
 *   row 7 = bottom of board (WHITE's back row)
 *   col 0 = left side
 *   col 7 = right side
 * </pre>
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Extends Vaadin's Div component</li>
 *   <li>Fixed size 60x60 pixels</li>
 *   <li>CSS handles piece rendering via pseudo-elements</li>
 *   <li>Click events handled by parent {@link BoardView}</li>
 * </ul>
 *
 * @see BoardView
 */
public class BoardSquare extends Div {

    private final int row;
    private final int col;
    private boolean highlighted;
    private boolean selected;

    public BoardSquare(int row, int col) {
        this.row = row;
        this.col = col;
        this.highlighted = false;
        this.selected = false;

        addClassName("board-square");
        addClassName((row + col) % 2 == 0 ? "light-square" : "dark-square");

        setWidth("60px");
        setHeight("60px");
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setPiece(Piece piece) {
        // Remove existing piece display
        removeClassName("white-piece");
        removeClassName("black-piece");
        removeClassName("king-piece");
        getElement().removeAttribute("data-piece");

        if (piece == null) {
            return;
        }

        // Add piece classes
        if (piece.getOwner() == Player.WHITE) {
            addClassName("white-piece");
        } else {
            addClassName("black-piece");
        }

        if (piece.getType() == PieceType.KING) {
            addClassName("king-piece");
        }

        getElement().setAttribute("data-piece", piece.getOwner().name().toLowerCase());
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        if (highlighted) {
            addClassName("highlighted");
        } else {
            removeClassName("highlighted");
        }
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            addClassName("selected");
        } else {
            removeClassName("selected");
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void clearHighlights() {
        setHighlighted(false);
        setSelected(false);
    }
}
