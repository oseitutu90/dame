package com.dame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoveCalculatorTest {

    private Board board;
    private MoveCalculator calculator;

    @BeforeEach
    void setUp() {
        board = new Board();
        calculator = new MoveCalculator(board);
    }

    @Nested
    @DisplayName("Man Simple Moves")
    class ManSimpleMoves {

        @Test
        @DisplayName("white man should move diagonally forward (up)")
        void whiteShouldMoveUp() {
            board.set(5, 2, new Piece(Player.WHITE));

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            assertThat(moves).hasSize(2);
            assertThat(moves).anyMatch(m -> m.getEndRow() == 4 && m.getEndCol() == 1);
            assertThat(moves).anyMatch(m -> m.getEndRow() == 4 && m.getEndCol() == 3);
        }

        @Test
        @DisplayName("black man should move diagonally forward (down)")
        void blackShouldMoveDown() {
            board.set(2, 3, new Piece(Player.BLACK));

            List<Move> moves = calculator.getValidMoves(Player.BLACK);

            assertThat(moves).hasSize(2);
            assertThat(moves).anyMatch(m -> m.getEndRow() == 3 && m.getEndCol() == 2);
            assertThat(moves).anyMatch(m -> m.getEndRow() == 3 && m.getEndCol() == 4);
        }

        @Test
        @DisplayName("should not move to occupied square")
        void shouldNotMoveToOccupied() {
            board.set(5, 2, new Piece(Player.WHITE));
            board.set(4, 1, new Piece(Player.WHITE)); // Blocking left diagonal
            board.set(4, 3, new Piece(Player.WHITE)); // Blocking right diagonal (own piece)

            // Get moves only for the piece at (5,2)
            List<Move> moves = calculator.getMovesForPiece(5, 2, board.get(5, 2));

            assertThat(moves).isEmpty();
        }

        @Test
        @DisplayName("should not move off board")
        void shouldNotMoveOffBoard() {
            board.set(0, 1, new Piece(Player.WHITE)); // Already at top

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            assertThat(moves).isEmpty();
        }
    }

    @Nested
    @DisplayName("Man Captures")
    class ManCaptures {

        @Test
        @DisplayName("should capture enemy piece diagonally")
        void shouldCaptureEnemy() {
            board.set(5, 2, new Piece(Player.WHITE));
            board.set(4, 3, new Piece(Player.BLACK));

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            assertThat(moves).hasSize(1);
            assertThat(moves.get(0).isCapture()).isTrue();
            assertThat(moves.get(0).getEndRow()).isEqualTo(3);
            assertThat(moves.get(0).getEndCol()).isEqualTo(4);
        }

        @Test
        @DisplayName("Ghanaian rule: man can capture backward")
        void manCanCaptureBackward() {
            board.set(3, 2, new Piece(Player.WHITE));
            board.set(4, 3, new Piece(Player.BLACK)); // Behind white piece

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            assertThat(moves).anyMatch(m ->
                m.isCapture() && m.getEndRow() == 5 && m.getEndCol() == 4
            );
        }

        @Test
        @DisplayName("captures are mandatory")
        void capturesAreMandatory() {
            board.set(5, 2, new Piece(Player.WHITE));
            board.set(4, 3, new Piece(Player.BLACK)); // Can capture
            // Also has simple moves available at 4,1

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            // Should only return capture moves
            assertThat(moves).allMatch(Move::isCapture);
        }

        @Test
        @DisplayName("should not capture own piece")
        void shouldNotCaptureOwn() {
            board.set(5, 2, new Piece(Player.WHITE));
            board.set(4, 3, new Piece(Player.WHITE)); // Own piece

            // Get moves only for the piece at (5,2)
            List<Move> moves = calculator.getMovesForPiece(5, 2, board.get(5, 2));

            // Should not be able to jump over own piece to (3,4)
            assertThat(moves).noneMatch(m -> m.getEndRow() == 3 && m.getEndCol() == 4);
        }
    }

    @Nested
    @DisplayName("Multi-Jump Captures")
    class MultiJumpCaptures {

        @Test
        @DisplayName("should find multi-jump sequence")
        void shouldFindMultiJump() {
            board.set(5, 0, new Piece(Player.WHITE));
            board.set(4, 1, new Piece(Player.BLACK));
            board.set(2, 3, new Piece(Player.BLACK));

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            // Should find the double capture
            assertThat(moves).anyMatch(m -> m.getCaptureCount() == 2);
        }
    }

    @Nested
    @DisplayName("King Moves")
    class KingMoves {

        @Test
        @DisplayName("Ghanaian rule: flying king moves any distance")
        void flyingKingMovesAnyDistance() {
            board.set(4, 4, new Piece(Player.WHITE, PieceType.KING));

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            // Should be able to move to many squares along diagonals
            assertThat(moves.size()).isGreaterThan(4);

            // Can reach corners
            assertThat(moves).anyMatch(m -> m.getEndRow() == 0 && m.getEndCol() == 0);
            assertThat(moves).anyMatch(m -> m.getEndRow() == 7 && m.getEndCol() == 7);
        }

        @Test
        @DisplayName("king should stop at pieces")
        void kingShouldStopAtPieces() {
            board.set(4, 4, new Piece(Player.WHITE, PieceType.KING));
            board.set(2, 2, new Piece(Player.WHITE)); // Blocking piece

            // Get moves only for the king at (4,4)
            List<Move> moves = calculator.getMovesForPiece(4, 4, board.get(4, 4));

            // Should not be able to reach 0,0 or 1,1 (blocked by piece at 2,2)
            assertThat(moves).noneMatch(m -> m.getEndRow() == 0 && m.getEndCol() == 0);
            assertThat(moves).noneMatch(m -> m.getEndRow() == 1 && m.getEndCol() == 1);

            // But can reach 3,3 (before the blocking piece)
            assertThat(moves).anyMatch(m -> m.getEndRow() == 3 && m.getEndCol() == 3);
        }
    }

    @Nested
    @DisplayName("King Captures")
    class KingCaptures {

        @Test
        @DisplayName("flying king can capture from distance")
        void flyingKingCapturesFromDistance() {
            board.set(7, 0, new Piece(Player.WHITE, PieceType.KING));
            board.set(4, 3, new Piece(Player.BLACK)); // Enemy at distance

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            // Should be able to capture and land on multiple squares beyond
            assertThat(moves).allMatch(Move::isCapture);
            assertThat(moves).anyMatch(m -> m.getEndRow() == 3 && m.getEndCol() == 4);
            assertThat(moves).anyMatch(m -> m.getEndRow() == 2 && m.getEndCol() == 5);
        }
    }

    @Nested
    @DisplayName("Game Start Position")
    class GameStartPosition {

        @Test
        @DisplayName("white should have valid opening moves")
        void whiteShouldHaveOpeningMoves() {
            board.setupInitialPosition();

            List<Move> moves = calculator.getValidMoves(Player.WHITE);

            // White pieces on row 5 can move forward
            assertThat(moves).isNotEmpty();
            assertThat(moves).allMatch(m -> m.getStartRow() == 5);
        }

        @Test
        @DisplayName("black should have valid opening moves")
        void blackShouldHaveOpeningMoves() {
            board.setupInitialPosition();

            List<Move> moves = calculator.getValidMoves(Player.BLACK);

            // Black pieces on row 2 can move forward
            assertThat(moves).isNotEmpty();
            assertThat(moves).allMatch(m -> m.getStartRow() == 2);
        }
    }
}
