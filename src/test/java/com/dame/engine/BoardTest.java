package com.dame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    @Nested
    @DisplayName("Initial Setup")
    class InitialSetup {

        @Test
        @DisplayName("should start with empty board")
        void shouldStartEmpty() {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    assertThat(board.get(r, c)).isNull();
                }
            }
        }

        @Test
        @DisplayName("should setup standard position correctly")
        void shouldSetupStandardPosition() {
            board.setupInitialPosition();

            // Black pieces in rows 0-2
            assertThat(board.countPieces(Player.BLACK)).isEqualTo(12);

            // White pieces in rows 5-7
            assertThat(board.countPieces(Player.WHITE)).isEqualTo(12);

            // Middle rows empty
            for (int r = 3; r < 5; r++) {
                for (int c = 0; c < 8; c++) {
                    assertThat(board.get(r, c)).isNull();
                }
            }
        }

        @Test
        @DisplayName("should place pieces only on dark squares")
        void shouldPlacePiecesOnDarkSquares() {
            board.setupInitialPosition();

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Piece piece = board.get(r, c);
                    if (piece != null) {
                        assertThat(board.isDarkSquare(r, c)).isTrue();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Piece Operations")
    class PieceOperations {

        @Test
        @DisplayName("should set and get piece correctly")
        void shouldSetAndGetPiece() {
            Piece piece = new Piece(Player.WHITE);
            board.set(3, 4, piece);

            assertThat(board.get(3, 4)).isEqualTo(piece);
        }

        @Test
        @DisplayName("should remove piece correctly")
        void shouldRemovePiece() {
            Piece piece = new Piece(Player.BLACK);
            board.set(2, 3, piece);
            board.remove(2, 3);

            assertThat(board.get(2, 3)).isNull();
        }

        @Test
        @DisplayName("should move piece correctly")
        void shouldMovePiece() {
            Piece piece = new Piece(Player.WHITE);
            board.set(5, 2, piece);

            board.movePiece(new Position(5, 2), new Position(4, 3));

            assertThat(board.get(5, 2)).isNull();
            assertThat(board.get(4, 3)).isEqualTo(piece);
        }
    }

    @Nested
    @DisplayName("Boundary Checks")
    class BoundaryChecks {

        @Test
        @DisplayName("should identify inside positions")
        void shouldIdentifyInsidePositions() {
            assertThat(board.isInside(0, 0)).isTrue();
            assertThat(board.isInside(7, 7)).isTrue();
            assertThat(board.isInside(3, 4)).isTrue();
        }

        @Test
        @DisplayName("should identify outside positions")
        void shouldIdentifyOutsidePositions() {
            assertThat(board.isInside(-1, 0)).isFalse();
            assertThat(board.isInside(0, -1)).isFalse();
            assertThat(board.isInside(8, 0)).isFalse();
            assertThat(board.isInside(0, 8)).isFalse();
        }

        @Test
        @DisplayName("should return null for outside positions")
        void shouldReturnNullForOutside() {
            assertThat(board.get(-1, 0)).isNull();
            assertThat(board.get(8, 8)).isNull();
        }
    }

    @Nested
    @DisplayName("Board Copy")
    class BoardCopy {

        @Test
        @DisplayName("should create independent copy")
        void shouldCreateIndependentCopy() {
            board.setupInitialPosition();
            Board copy = board.copy();

            // Modify original
            board.remove(0, 1);

            // Copy should be unchanged
            assertThat(copy.get(0, 1)).isNotNull();
        }
    }
}
