package com.dame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameHistoryTest {

    private GameLogic game;

    @BeforeEach
    void setUp() {
        game = new GameLogic();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("canUndo should return false initially")
        void canUndoShouldReturnFalseInitially() {
            assertThat(game.canUndo()).isFalse();
        }

        @Test
        @DisplayName("undo should return false when no history")
        void undoShouldReturnFalseWhenNoHistory() {
            assertThat(game.undo()).isFalse();
        }
    }

    @Nested
    @DisplayName("Simple Move Undo")
    class SimpleMoveUndo {

        @Test
        @DisplayName("should enable undo after a move")
        void shouldEnableUndoAfterMove() {
            game.applyMove(game.getValidMoves().get(0));

            assertThat(game.canUndo()).isTrue();
        }

        @Test
        @DisplayName("should restore previous player after undo")
        void shouldRestorePreviousPlayerAfterUndo() {
            assertThat(game.getCurrentPlayer()).isEqualTo(Player.WHITE);

            game.applyMove(game.getValidMoves().get(0));
            assertThat(game.getCurrentPlayer()).isEqualTo(Player.BLACK);

            game.undo();
            assertThat(game.getCurrentPlayer()).isEqualTo(Player.WHITE);
        }

        @Test
        @DisplayName("should restore piece position after undo")
        void shouldRestorePiecePositionAfterUndo() {
            Move move = game.getValidMoves().get(0);
            Position start = move.getStart();
            Position end = move.getEnd();

            // Before move: piece at start, empty at end
            assertThat(game.getBoard().get(start)).isNotNull();
            assertThat(game.getBoard().get(end)).isNull();

            game.applyMove(move);

            // After move: empty at start, piece at end
            assertThat(game.getBoard().get(start)).isNull();
            assertThat(game.getBoard().get(end)).isNotNull();

            game.undo();

            // After undo: piece back at start, empty at end
            assertThat(game.getBoard().get(start)).isNotNull();
            assertThat(game.getBoard().get(end)).isNull();
        }

        @Test
        @DisplayName("canUndo should return false after undoing single move")
        void canUndoShouldReturnFalseAfterUndoingSingleMove() {
            game.applyMove(game.getValidMoves().get(0));
            game.undo();

            assertThat(game.canUndo()).isFalse();
        }
    }

    @Nested
    @DisplayName("Capture Move Undo")
    class CaptureMoveUndo {

        @Test
        @DisplayName("should restore captured piece after undo")
        void shouldRestoreCapturedPieceAfterUndo() {
            // Set up a capture scenario
            Board board = game.getBoard();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // White piece that can capture
            board.set(5, 2, new Piece(Player.WHITE));
            board.set(4, 3, new Piece(Player.BLACK)); // Enemy to capture
            board.set(0, 1, new Piece(Player.BLACK)); // Extra piece so game doesn't end

            int initialBlackCount = board.countPieces(Player.BLACK);
            assertThat(initialBlackCount).isEqualTo(2);

            // Execute capture
            List<Move> moves = game.getValidMoves();
            Move capture = moves.stream().filter(Move::isCapture).findFirst().orElse(null);
            assertThat(capture).isNotNull();

            game.applyMove(capture);

            // After capture: one less black piece
            assertThat(game.getBoard().countPieces(Player.BLACK)).isEqualTo(1);

            // Undo
            game.undo();

            // After undo: black pieces restored
            assertThat(game.getBoard().countPieces(Player.BLACK)).isEqualTo(2);
            assertThat(game.getBoard().get(4, 3)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Multiple Undo")
    class MultipleUndo {

        @Test
        @DisplayName("should handle multiple undos")
        void shouldHandleMultipleUndos() {
            // Make three moves
            game.applyMove(game.getValidMoves().get(0)); // White
            game.applyMove(game.getValidMoves().get(0)); // Black
            game.applyMove(game.getValidMoves().get(0)); // White

            assertThat(game.getCurrentPlayer()).isEqualTo(Player.BLACK);

            // Undo all three
            assertThat(game.undo()).isTrue();
            assertThat(game.getCurrentPlayer()).isEqualTo(Player.WHITE);

            assertThat(game.undo()).isTrue();
            assertThat(game.getCurrentPlayer()).isEqualTo(Player.BLACK);

            assertThat(game.undo()).isTrue();
            assertThat(game.getCurrentPlayer()).isEqualTo(Player.WHITE);

            assertThat(game.canUndo()).isFalse();
        }

        @Test
        @DisplayName("should restore original board after undoing all moves")
        void shouldRestoreOriginalBoardAfterUndoingAllMoves() {
            int initialWhite = game.getBoard().countPieces(Player.WHITE);
            int initialBlack = game.getBoard().countPieces(Player.BLACK);

            // Make some moves
            game.applyMove(game.getValidMoves().get(0));
            game.applyMove(game.getValidMoves().get(0));

            // Undo all
            game.undo();
            game.undo();

            assertThat(game.getBoard().countPieces(Player.WHITE)).isEqualTo(initialWhite);
            assertThat(game.getBoard().countPieces(Player.BLACK)).isEqualTo(initialBlack);
        }
    }

    @Nested
    @DisplayName("Reset Clears History")
    class ResetClearsHistory {

        @Test
        @DisplayName("reset should clear undo history")
        void resetShouldClearUndoHistory() {
            game.applyMove(game.getValidMoves().get(0));
            assertThat(game.canUndo()).isTrue();

            game.reset();

            assertThat(game.canUndo()).isFalse();
        }
    }

    @Nested
    @DisplayName("Promotion Undo")
    class PromotionUndo {

        @Test
        @DisplayName("should demote king back to man after undo")
        void shouldDemoteKingBackToManAfterUndo() {
            Board board = game.getBoard();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // White piece one step from promotion
            board.set(1, 2, new Piece(Player.WHITE));
            board.set(7, 0, new Piece(Player.BLACK)); // Keep game going

            Piece piece = board.get(1, 2);
            assertThat(piece.isKing()).isFalse();

            // Make promotion move
            Move promotionMove = game.getValidMoves().stream()
                    .filter(m -> m.getEndRow() == 0)
                    .findFirst()
                    .orElse(null);

            assertThat(promotionMove).isNotNull();
            game.applyMove(promotionMove);

            // After promotion
            assertThat(game.getBoard().get(promotionMove.getEnd()).isKing()).isTrue();

            // Undo
            game.undo();

            // Should be a man again at original position
            Piece restored = game.getBoard().get(1, 2);
            assertThat(restored).isNotNull();
            assertThat(restored.isKing()).isFalse();
        }
    }
}
