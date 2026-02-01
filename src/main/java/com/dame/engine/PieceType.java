package com.dame.engine;

/**
 * Represents the type of a game piece in Dame (Checkers).
 *
 * <h2>Piece Types</h2>
 * <table border="1">
 *   <tr><th>Type</th><th>Movement</th><th>Capture</th></tr>
 *   <tr>
 *     <td>MAN</td>
 *     <td>1 square diagonally forward only</td>
 *     <td>All 4 diagonal directions (Ghanaian rule)</td>
 *   </tr>
 *   <tr>
 *     <td>KING</td>
 *     <td>Any distance diagonally ("flying king")</td>
 *     <td>Any distance diagonally, can land anywhere beyond captured piece</td>
 *   </tr>
 * </table>
 *
 * <h2>Promotion</h2>
 * A MAN becomes a KING when it reaches the opponent's back row:
 * <ul>
 *   <li>WHITE promotes at row 0 (top of board)</li>
 *   <li>BLACK promotes at row 7 (bottom of board)</li>
 * </ul>
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Stored in each {@link Piece} instance</li>
 *   <li>Type can only change MAN → KING (never reversed)</li>
 *   <li>Serialized to JSON by {@link BoardStateSerializer} for persistence</li>
 * </ul>
 *
 * @see Piece#promoteToKing()
 * @see MoveCalculator
 */
public enum PieceType {
    /** Regular piece - moves forward only, captures in all diagonal directions */
    MAN,

    /** Promoted piece - "flying king" with unlimited diagonal range */
    KING
}
