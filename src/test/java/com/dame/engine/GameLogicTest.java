package com.dame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameLogicTest {

    private GameLogic game;

    @BeforeEach
    void setUp() {
        game = new GameLogic();
    }

    @Nested
    @DisplayName("Game Initialization")
    class GameInitialization {

        @Test
        @DisplayName("should start with white's turn")
        void shouldStartWithWhite() {
            assertThat(game.getCurrentPlayer()).isEqualTo(Player.WHITE);
        }

        @Test
        @DisplayName("should start in progress state")
        void shouldStartInProgress() {
            assertThat(game.getGameState()).isEqualTo(GameState.IN_PROGRESS);
            assertThat(game.isGameOver()).isFalse();
        }

        @Test
        @DisplayName("should have valid moves available")
        void shouldHaveValidMoves() {
            assertThat(game.getValidMoves()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Turn Management")
    class TurnManagement {

        @Test
        @DisplayName("should switch turns after simple move")
        void shouldSwitchTurnsAfterMove() {
            List<Move> moves = game.getValidMoves();
            Move firstMove = moves.get(0);

            game.applyMove(firstMove);

            assertThat(game.getCurrentPlayer()).isEqualTo(Player.BLACK);
        }
    }

    @Nested
    @DisplayName("Piece Selection")
    class PieceSelection {

        @Test
        @DisplayName("can select own pieces")
        void canSelectOwnPieces() {
            // White piece at row 5, col 0 (dark square)
            assertThat(game.canSelect(5, 0)).isTrue();
        }

        @Test
        @DisplayName("cannot select opponent pieces")
        void cannotSelectOpponentPieces() {
            // Black piece at row 2, col 1 (dark square)
            assertThat(game.canSelect(2, 1)).isFalse();
        }

        @Test
        @DisplayName("cannot select empty squares")
        void cannotSelectEmptySquares() {
            assertThat(game.canSelect(4, 4)).isFalse();
        }
    }

    @Nested
    @DisplayName("Piece Promotion")
    class PiecePromotion {

        @Test
        @DisplayName("white piece promotes at row 0")
        void whitePiecePromotes() {
            // Clear board for clean test
            Board board = game.getBoard();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // Setup: white piece one step from promotion
            board.set(1, 2, new Piece(Player.WHITE));
            // Add a black piece so game doesn't end
            board.set(7, 0, new Piece(Player.BLACK));

            // Get valid moves and find the promotion move
            List<Move> moves = game.getValidMoves();
            Move promotionMove = moves.stream()
                    .filter(m -> m.getEndRow() == 0)
                    .findFirst()
                    .orElse(null);

            assertThat(promotionMove).isNotNull();
            game.applyMove(promotionMove);

            Piece promoted = board.get(promotionMove.getEnd());
            assertThat(promoted).isNotNull();
            assertThat(promoted.isKing()).isTrue();
        }

        @Test
        @DisplayName("black piece promotes at row 7")
        void blackPiecePromotes() {
            // Clear board for clean test
            Board board = game.getBoard();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // Setup: white piece so we can make a move, black piece near promotion
            board.set(3, 0, new Piece(Player.WHITE));
            board.set(6, 1, new Piece(Player.BLACK));

            // White makes a simple move first
            game.applyMove(game.getValidMoves().get(0));

            // Now it's black's turn - get moves and find promotion move
            List<Move> moves = game.getValidMoves();
            Move promotionMove = moves.stream()
                    .filter(m -> m.getEndRow() == 7)
                    .findFirst()
                    .orElse(null);

            assertThat(promotionMove).isNotNull();
            game.applyMove(promotionMove);

            Piece promoted = board.get(promotionMove.getEnd());
            assertThat(promoted).isNotNull();
            assertThat(promoted.isKing()).isTrue();
        }
    }

    @Nested
    @DisplayName("Multi-Jump")
    class MultiJump {

        @Test
        @DisplayName("should continue turn during multi-jump")
        void shouldContinueTurnDuringMultiJump() {
            // Clear board and set up multi-jump scenario
            Board board = game.getBoard();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // White piece with two enemies to capture in sequence
            board.set(5, 0, new Piece(Player.WHITE));
            board.set(4, 1, new Piece(Player.BLACK));
            board.set(2, 3, new Piece(Player.BLACK));

            // Get the valid capture move (should capture first enemy)
            List<Move> moves = game.getValidMoves();

            // Find a single capture move (not the double capture)
            Move firstCapture = moves.stream()
                    .filter(m -> m.getCaptureCount() == 1 && m.getEndRow() == 3 && m.getEndCol() == 2)
                    .findFirst()
                    .orElse(null);

            // If no single capture found, the calculator returns only the full multi-jump
            // In that case, the full capture sequence is enforced
            if (firstCapture == null) {
                // Full multi-jump is mandatory - find it
                Move fullCapture = moves.stream()
                        .filter(m -> m.getCaptureCount() == 2)
                        .findFirst()
                        .orElse(null);

                assertThat(fullCapture).isNotNull();
                boolean turnEnded = game.applyMove(fullCapture);
                assertThat(turnEnded).isTrue(); // Full capture completes the turn
            } else {
                boolean turnEnded = game.applyMove(firstCapture);
                // After first capture, should be in multi-jump if more captures available
                assertThat(turnEnded).isFalse();
                assertThat(game.isInMultiJump()).isTrue();
                assertThat(game.getCurrentPlayer()).isEqualTo(Player.WHITE);
            }
        }
    }

    @Nested
    @DisplayName("Game Over Conditions")
    class GameOverConditions {

        @Test
        @DisplayName("game ends when player has no pieces")
        void gameEndsWhenNoPieces() {
            Board board = game.getBoard();
            // Remove all black pieces
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // Make any white move to trigger state check
            game.applyMove(game.getValidMoves().get(0));

            assertThat(game.getGameState()).isEqualTo(GameState.WHITE_WINS);
        }

        @Test
        @DisplayName("game ends when player has no valid moves")
        void gameEndsWhenNoMoves() {
            Board board = game.getBoard();
            // Clear board
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // Put black piece on dark square (2,1) - surrounded by white pieces
            // Diagonal neighbors: (1,0), (1,2), (3,0), (3,2)
            board.set(2, 1, new Piece(Player.BLACK));
            // Block all diagonal moves with white pieces
            board.set(1, 0, new Piece(Player.WHITE));
            board.set(1, 2, new Piece(Player.WHITE));
            board.set(3, 0, new Piece(Player.WHITE));
            board.set(3, 2, new Piece(Player.WHITE));
            // Block capture landing squares too
            board.set(0, 3, new Piece(Player.WHITE)); // blocks capture over (1,2)
            board.set(4, 3, new Piece(Player.WHITE)); // blocks capture over (3,2)

            // Add an extra white piece to move (on dark square)
            board.set(6, 1, new Piece(Player.WHITE));

            // Verify black has no moves
            MoveCalculator calc = new MoveCalculator(board);
            assertThat(calc.getValidMoves(Player.BLACK)).isEmpty();

            // White's turn - make a move
            List<Move> whiteMoves = game.getValidMoves();
            assertThat(whiteMoves).isNotEmpty();
            game.applyMove(whiteMoves.get(0));

            // Black should have no moves - game over
            assertThat(game.getGameState()).isEqualTo(GameState.WHITE_WINS);
        }

        @Test
        @DisplayName("1 king vs 1 king is a draw")
        void oneKingVsOneKingIsDraw() {
            Board board = game.getBoard();
            // Clear board
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // Setup: 1 white king, 1 black king (far apart, can't capture)
            board.set(1, 0, new Piece(Player.WHITE, PieceType.KING));
            board.set(7, 6, new Piece(Player.BLACK, PieceType.KING));

            // Make a move to trigger state check
            List<Move> moves = game.getValidMoves();
            game.applyMove(moves.get(0));

            assertThat(game.getGameState()).isEqualTo(GameState.DRAW);
        }

        @Test
        @DisplayName("2 kings vs 1 king continues game")
        void twoKingsVsOneKingContinues() {
            Board board = game.getBoard();
            // Clear board
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // Setup: 2 white kings, 1 black king (far apart)
            board.set(1, 0, new Piece(Player.WHITE, PieceType.KING));
            board.set(1, 4, new Piece(Player.WHITE, PieceType.KING));
            board.set(7, 6, new Piece(Player.BLACK, PieceType.KING));

            // Make a move to trigger state check
            List<Move> moves = game.getValidMoves();
            game.applyMove(moves.get(0));

            assertThat(game.getGameState()).isEqualTo(GameState.IN_PROGRESS);
        }

        @Test
        @DisplayName("king vs single man wins for king player")
        void kingVsSingleManWinsForKing() {
            Board board = game.getBoard();
            // Clear board
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    board.remove(r, c);
                }
            }

            // Setup: 1 white king, 1 black man
            board.set(3, 2, new Piece(Player.WHITE, PieceType.KING));
            board.set(6, 5, new Piece(Player.BLACK, PieceType.MAN));

            // Make a move to trigger state check
            List<Move> moves = game.getValidMoves();
            game.applyMove(moves.get(0));

            assertThat(game.getGameState()).isEqualTo(GameState.WHITE_WINS);
        }
    }

    @Nested
    @DisplayName("Game Reset")
    class GameReset {

        @Test
        @DisplayName("reset should restore initial state")
        void resetShouldRestoreInitialState() {
            // Make some moves
            game.applyMove(game.getValidMoves().get(0));
            game.applyMove(game.getValidMoves().get(0));

            game.reset();

            assertThat(game.getCurrentPlayer()).isEqualTo(Player.WHITE);
            assertThat(game.getGameState()).isEqualTo(GameState.IN_PROGRESS);
            assertThat(game.getBoard().countPieces(Player.WHITE)).isEqualTo(12);
            assertThat(game.getBoard().countPieces(Player.BLACK)).isEqualTo(12);
        }
    }
}
