package com.dame.engine;

/**
 * Represents the current state of a single game within a match.
 *
 * <h2>State Transitions</h2>
 * <pre>
 *                    ┌──────────────┐
 *                    │ IN_PROGRESS  │
 *                    └──────┬───────┘
 *          ┌─────────────┬──┴───┬─────────────┐
 *          ▼             ▼      ▼             ▼
 *   ┌────────────┐ ┌─────────┐ ┌────────────┐ ┌──────┐
 *   │ WHITE_WINS │ │  DRAW   │ │ BLACK_WINS │ │(stay)│
 *   └────────────┘ └─────────┘ └────────────┘ └──────┘
 * </pre>
 *
 * <h2>Win/Loss Conditions (Ghanaian Dame Rules)</h2>
 * <ul>
 *   <li><b>No valid moves:</b> Player with no legal moves loses</li>
 *   <li><b>No pieces:</b> Player with 0 pieces loses</li>
 *   <li><b>King vs single man:</b> King player wins immediately</li>
 * </ul>
 *
 * <h2>Draw Conditions</h2>
 * <ul>
 *   <li><b>King vs King:</b> Both players have exactly 1 king each (no other pieces)</li>
 * </ul>
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Used with Java 17+ switch expressions (arrow syntax) for clean branching</li>
 *   <li>Stored in {@link GameSnapshot} for undo functionality</li>
 *   <li>Recorded by {@link MatchScore} to track series progress</li>
 * </ul>
 *
 * @see GameLogic#updateGameState()
 * @see MatchScore#recordGameResult(GameState)
 */
public enum GameState {
    /** Game is ongoing, moves can still be made */
    IN_PROGRESS,

    /** White player has won this game */
    WHITE_WINS,

    /** Black player has won this game */
    BLACK_WINS,

    /** Game ended in a draw (e.g., king vs king) */
    DRAW
}
