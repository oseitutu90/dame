package com.dame.engine;

/**
 * Represents a game piece with an owner and type.
 * Can be a man or a king.
 */
public class Piece {

    private final Player owner;
    private PieceType type;

    public Piece(Player owner) {
        this.owner = owner;
        this.type = PieceType.MAN;
    }

    public Piece(Player owner, PieceType type) {
        this.owner = owner;
        this.type = type;
    }

    public Player getOwner() {
        return owner;
    }

    public PieceType getType() {
        return type;
    }

    public void promoteToKing() {
        this.type = PieceType.KING;
    }

    public boolean isKing() {
        return type == PieceType.KING;
    }

    public Piece copy() {
        return new Piece(owner, type);
    }
}
