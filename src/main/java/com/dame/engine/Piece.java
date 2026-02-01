package com.dame.engine;

/**
 * Represents a game piece with an owner (WHITE/BLACK) and type (MAN/KING).
 *
 * <h2>Piece Lifecycle</h2>
 * <pre>
 *   new Piece(Player) → MAN (default)
 *            │
 *            ▼ reaches opponent's back row
 *   promoteToKing() → KING (permanent)
 * </pre>
 *
 * <h2>Movement Rules (Ghanaian Dame)</h2>
 * <table border="1">
 *   <tr><th>Type</th><th>Simple Move</th><th>Capture</th></tr>
 *   <tr>
 *     <td>MAN</td>
 *     <td>1 square forward diagonally</td>
 *     <td>Can capture in ALL 4 diagonal directions (forward & backward)</td>
 *   </tr>
 *   <tr>
 *     <td>KING</td>
 *     <td>Any distance in any diagonal direction ("flying king")</td>
 *     <td>Can capture from distance, land anywhere beyond captured piece</td>
 *   </tr>
 * </table>
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Owner is immutable (final) - pieces never change sides</li>
 *   <li>Type is mutable - can be promoted from MAN to KING</li>
 *   <li>{@link #copy()} creates a deep copy for board state snapshots</li>
 *   <li>Stored in {@link Board}'s 2D grid array</li>
 * </ul>
 *
 * @see Board
 * @see MoveCalculator
 * @see GameLogic
 */
public class Piece {

    /** The player who owns this piece (immutable) */
    private final Player owner;

    /** The piece type - MAN or KING (mutable via promotion) */
    private PieceType type;

    /**
     * Creates a new MAN piece for the specified player.
     *
     * @param owner the player who owns this piece
     */
    public Piece(Player owner) {
        this.owner = owner;
        this.type = PieceType.MAN;
    }

    /**
     * Creates a piece with explicit type (used for deserialization and copying).
     *
     * @param owner the player who owns this piece
     * @param type  the piece type (MAN or KING)
     */
    public Piece(Player owner, PieceType type) {
        this.owner = owner;
        this.type = type;
    }

    /**
     * @return the player who owns this piece
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * @return the current piece type (MAN or KING)
     */
    public PieceType getType() {
        return type;
    }

    /**
     * Promotes this piece from MAN to KING.
     * Called when a MAN reaches the opponent's back row.
     *
     * <p>Note: This is a one-way operation. Kings cannot be demoted.</p>
     */
    public void promoteToKing() {
        this.type = PieceType.KING;
    }

    /**
     * @return true if this piece is a KING
     */
    public boolean isKing() {
        return type == PieceType.KING;
    }

    /**
     * Creates a deep copy of this piece.
     * Used for creating board snapshots for undo functionality.
     *
     * @return a new Piece with the same owner and type
     */
    public Piece copy() {
        return new Piece(owner, type);
    }
}
