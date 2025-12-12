package com.dame.ui;

import com.dame.engine.Piece;
import com.dame.engine.PieceType;
import com.dame.engine.Player;
import com.vaadin.flow.component.html.Div;

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
