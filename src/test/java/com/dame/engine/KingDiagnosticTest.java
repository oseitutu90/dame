package com.dame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Diagnostic tests for king movement and capture logic
 */
class KingDiagnosticTest {

    private Board board;
    private MoveCalculator calculator;

    @BeforeEach
    void setUp() {
        board = new Board();
        calculator = new MoveCalculator(board);
    }

    @Test
    @DisplayName("DIAGNOSTIC: King should fly any distance diagonally")
    void kingFliesAnyDistance() {
        // Place white king in center
        board.set(4, 4, new Piece(Player.WHITE, PieceType.KING));

        List<Move> moves = calculator.getValidMoves(Player.WHITE);

        System.out.println("=== KING FLYING MOVES TEST ===");
        System.out.println("King at (4,4)");
        System.out.println("Total moves: " + moves.size());

        for (Move m : moves) {
            System.out.println("  " + m);
        }

        // Should have many moves reaching corners
        assertThat(moves).isNotEmpty();

        // Check specific diagonal squares
        // Up-left diagonal: (3,3), (2,2), (1,1), (0,0)
        assertThat(moves).anyMatch(m -> m.getEndRow() == 0 && m.getEndCol() == 0);
        // Up-right diagonal: (3,5), (2,6), (1,7)
        assertThat(moves).anyMatch(m -> m.getEndRow() == 1 && m.getEndCol() == 7);
        // Down-left diagonal: (5,3), (6,2), (7,1)
        assertThat(moves).anyMatch(m -> m.getEndRow() == 7 && m.getEndCol() == 1);
        // Down-right diagonal: (5,5), (6,6), (7,7)
        assertThat(moves).anyMatch(m -> m.getEndRow() == 7 && m.getEndCol() == 7);
    }

    @Test
    @DisplayName("DIAGNOSTIC: King capture from distance")
    void kingCapturesFromDistance() {
        // White king at corner, black piece at distance
        board.set(7, 0, new Piece(Player.WHITE, PieceType.KING));
        board.set(4, 3, new Piece(Player.BLACK)); // Enemy 3 squares away

        List<Move> moves = calculator.getValidMoves(Player.WHITE);

        System.out.println("=== KING CAPTURE FROM DISTANCE TEST ===");
        System.out.println("King at (7,0), Enemy at (4,3)");
        System.out.println("Total moves: " + moves.size());

        for (Move m : moves) {
            System.out.println("  " + m);
        }

        // All moves should be captures (mandatory)
        assertThat(moves).allMatch(Move::isCapture);

        // Should have multiple landing positions after capture
        assertThat(moves.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("DIAGNOSTIC: King blocked by own piece")
    void kingBlockedByOwnPiece() {
        // White king with own piece blocking one diagonal
        board.set(4, 4, new Piece(Player.WHITE, PieceType.KING));
        board.set(2, 2, new Piece(Player.WHITE)); // Blocking up-left

        List<Move> moves = calculator.getMovesForPiece(4, 4, board.get(4, 4));

        System.out.println("=== KING BLOCKED BY OWN TEST ===");
        System.out.println("King at (4,4), Own piece at (2,2)");
        System.out.println("Total moves: " + moves.size());

        for (Move m : moves) {
            System.out.println("  " + m);
        }

        // Should NOT have moves past (2,2)
        assertThat(moves).noneMatch(m -> m.getEndRow() == 0 && m.getEndCol() == 0);
        assertThat(moves).noneMatch(m -> m.getEndRow() == 1 && m.getEndCol() == 1);
        // But CAN reach (3,3) - one step before blocking piece
        assertThat(moves).anyMatch(m -> m.getEndRow() == 3 && m.getEndCol() == 3);
    }

    @Test
    @DisplayName("DIAGNOSTIC: King multi-capture")
    void kingMultiCapture() {
        // King with multiple enemies to capture
        board.set(7, 0, new Piece(Player.WHITE, PieceType.KING));
        board.set(5, 2, new Piece(Player.BLACK)); // First enemy
        board.set(2, 5, new Piece(Player.BLACK)); // Second enemy in path

        List<Move> moves = calculator.getValidMoves(Player.WHITE);

        System.out.println("=== KING MULTI-CAPTURE TEST ===");
        System.out.println("King at (7,0), Enemies at (5,2) and (2,5)");
        System.out.println("Total moves: " + moves.size());

        for (Move m : moves) {
            System.out.println("  " + m + " (captures: " + m.getCaptureCount() + ")");
        }

        // Should find multi-capture sequences
        boolean hasMultiCapture = moves.stream().anyMatch(m -> m.getCaptureCount() >= 2);
        System.out.println("Has multi-capture: " + hasMultiCapture);
    }

    @Test
    @DisplayName("DIAGNOSTIC: Simple board - king at start")
    void simpleKingTest() {
        board.set(3, 3, new Piece(Player.WHITE, PieceType.KING));

        List<Move> moves = calculator.getValidMoves(Player.WHITE);

        System.out.println("=== SIMPLE KING TEST ===");
        System.out.println("King at (3,3) - no other pieces");
        System.out.println("Total moves: " + moves.size());
        System.out.println(board);

        for (Move m : moves) {
            System.out.println("  " + m);
        }

        // King should have many moves in all directions
        assertThat(moves.size()).isGreaterThanOrEqualTo(9); // At least 9 empty diagonal squares
    }
}
