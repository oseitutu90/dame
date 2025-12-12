package com.dame.ui;

import com.dame.engine.Board;
import com.dame.engine.GameState;
import com.dame.engine.Move;
import com.dame.engine.Piece;
import com.dame.engine.Position;
import com.dame.service.DameService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("")
public class BoardView extends VerticalLayout {

    private final DameService gameService;
    private final BoardSquare[][] squares = new BoardSquare[8][8];
    private final Span statusLabel;
    private Button undoBtn;

    private BoardSquare selectedSquare;
    private List<Move> currentValidMoves;

    public BoardView(DameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("game-container");

        // Title
        H1 title = new H1("Ghanaian Checkers");
        title.addClassName("game-title");

        // Status label
        statusLabel = new Span();
        statusLabel.addClassName("status-label");

        // Board grid
        Div boardContainer = createBoardGrid();

        // Control buttons
        HorizontalLayout controls = createControls();

        add(title, statusLabel, boardContainer, controls);

        // Initial render
        refreshBoard();
    }

    private Div createBoardGrid() {
        Div boardContainer = new Div();
        boardContainer.addClassName("board-container");

        Div grid = new Div();
        grid.addClassName("board-grid");

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                BoardSquare square = new BoardSquare(row, col);
                square.addClickListener(e -> handleSquareClick(square));
                squares[row][col] = square;
                grid.add(square);
            }
        }

        boardContainer.add(grid);
        return boardContainer;
    }

    private HorizontalLayout createControls() {
        Button newGameBtn = new Button("New Game", e -> {
            gameService.newGame();
            clearSelection();
            refreshBoard();
        });
        newGameBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        undoBtn = new Button("Undo", e -> {
            if (gameService.undo()) {
                clearSelection();
                refreshBoard();
            }
        });
        undoBtn.setEnabled(false);

        return new HorizontalLayout(newGameBtn, undoBtn);
    }

    private void handleSquareClick(BoardSquare square) {
        if (gameService.isGameOver()) {
            return;
        }

        int row = square.getRow();
        int col = square.getCol();

        if (selectedSquare == null) {
            // No piece selected yet - try to select this square
            if (gameService.canSelect(row, col)) {
                selectSquare(square);
            }
        } else {
            // A piece is already selected
            if (square == selectedSquare) {
                // Clicked same square - deselect
                clearSelection();
            } else if (square.isHighlighted()) {
                // Clicked a valid move destination
                executeMove(row, col);
            } else if (gameService.canSelect(row, col)) {
                // Clicked another own piece - select it instead
                clearSelection();
                selectSquare(square);
            } else {
                // Clicked invalid square
                clearSelection();
            }
        }
    }

    private void selectSquare(BoardSquare square) {
        selectedSquare = square;
        square.setSelected(true);

        // Get and highlight valid moves
        currentValidMoves = gameService.getValidMovesFor(square.getRow(), square.getCol());
        for (Move move : currentValidMoves) {
            squares[move.getEndRow()][move.getEndCol()].setHighlighted(true);
        }
    }

    private void executeMove(int endRow, int endCol) {
        // Find the matching move
        Move selectedMove = currentValidMoves.stream()
                .filter(m -> m.getEndRow() == endRow && m.getEndCol() == endCol)
                .findFirst()
                .orElse(null);

        if (selectedMove != null) {
            boolean turnEnded = gameService.applyMove(selectedMove);

            clearSelection();
            refreshBoard();

            // If multi-jump continues, auto-select the jumping piece
            if (!turnEnded && gameService.isInMultiJump()) {
                Position jumpPos = gameService.getMultiJumpPosition();
                selectSquare(squares[jumpPos.row()][jumpPos.col()]);
            }
        }
    }

    private void clearSelection() {
        if (selectedSquare != null) {
            selectedSquare.setSelected(false);
            selectedSquare = null;
        }

        // Clear all highlights
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares[r][c].clearHighlights();
            }
        }

        currentValidMoves = null;
    }

    private void refreshBoard() {
        Board board = gameService.getBoard();

        // Update pieces on all squares
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.get(row, col);
                squares[row][col].setPiece(piece);
            }
        }

        // Update status
        updateStatus();

        // Update undo button state
        undoBtn.setEnabled(gameService.canUndo());
    }

    private void updateStatus() {
        statusLabel.setText(gameService.getStatusMessage());

        // Update status styling based on game state
        statusLabel.removeClassName("status-white");
        statusLabel.removeClassName("status-black");
        statusLabel.removeClassName("status-gameover");

        GameState state = gameService.getGameState();
        if (state == GameState.IN_PROGRESS) {
            String playerClass = gameService.getCurrentPlayer().name().toLowerCase();
            statusLabel.addClassName("status-" + playerClass);
        } else {
            statusLabel.addClassName("status-gameover");
        }
    }
}
