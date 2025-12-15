package com.dame.ui;

import com.dame.engine.Board;
import com.dame.engine.GameState;
import com.dame.engine.MatchScore;
import com.dame.engine.Move;
import com.dame.engine.Piece;
import com.dame.engine.Player;
import com.dame.engine.Position;
import com.dame.service.DameService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Play | Checkers")
@PermitAll
public class BoardView extends VerticalLayout {

    private final DameService gameService;
    private final BoardSquare[][] squares = new BoardSquare[8][8];
    private final Span statusLabel;
    private final Span scoreLabel;
    private final Span gameCountLabel;
    private final Span matchResultLabel;
    private Button newGameBtn;
    private Button undoBtn;
    private Button newMatchBtn;

    private BoardSquare selectedSquare;
    private List<Move> currentValidMoves;

    public BoardView(DameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("game-container");

        // ========== HEADER SECTION ==========
        Div header = new Div();
        header.addClassName("header-section");

        H1 title = new H1("Checkers");
        title.addClassName("game-title");

        header.add(title);

        // ========== MAIN CONTENT AREA (horizontal layout) ==========
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.addClassName("main-content");
        mainContent.setSizeFull();
        mainContent.setJustifyContentMode(JustifyContentMode.CENTER);
        mainContent.setAlignItems(Alignment.CENTER);
        mainContent.setSpacing(false);
        mainContent.setPadding(false);

        // --- CENTER: Game Board Area ---
        VerticalLayout gameArea = new VerticalLayout();
        gameArea.addClassName("game-area");
        gameArea.setAlignItems(Alignment.CENTER);
        gameArea.setSpacing(false);
        gameArea.setPadding(false);

        // Score display
        scoreLabel = new Span();
        scoreLabel.addClassName("score-label");

        // Game count display
        gameCountLabel = new Span();
        gameCountLabel.addClassName("game-count-label");

        // Match result label
        matchResultLabel = new Span();
        matchResultLabel.addClassName("match-result-label");
        matchResultLabel.setVisible(false);

        // Status label
        statusLabel = new Span();
        statusLabel.addClassName("status-label");

        // Board grid
        Div boardContainer = createBoardGrid();

        // Control buttons
        HorizontalLayout controls = createControls();

        gameArea.add(scoreLabel, gameCountLabel, matchResultLabel, statusLabel, boardContainer, controls);

        // --- RIGHT: Game Description Panel ---
        VerticalLayout sidePanel = new VerticalLayout();
        sidePanel.addClassName("side-panel");
        sidePanel.setSpacing(false);
        sidePanel.setPadding(false);

        Div rulesPanel = createRulesPanel();
        sidePanel.add(rulesPanel);

        // Assemble main content (board + description)
        mainContent.add(gameArea, sidePanel);

        // Add all sections to main layout
        add(header, mainContent);

        // Initial render
        refreshBoard();
    }

    private Div createRulesPanel() {
        Div panel = new Div();
        panel.addClassName("rules-panel");

        Span titleSpan = new Span("How Ghanaian Dame Differs");
        titleSpan.addClassName("rules-title");

        Div content = new Div();
        content.addClassName("rules-content");
        content.getElement().setProperty("innerHTML",
                "<strong>Dame</strong> (also called Damii) is Ghana's beloved checkers variant. " +
                        "Key differences from standard checkers:" +
                        "<ul class='rules-list'>" +
                        "<li><strong>Free Capture Choice:</strong> You may choose ANY capture sequence — " +
                        "no requirement to take the maximum pieces.</li>" +
                        "<li><strong>Huff Rule:</strong> Miss a mandatory capture? Your piece is forfeited!</li>" +
                        "<li><strong>Single Piece = Loss:</strong> If you're down to just one piece, you lose instantly.</li>"
                        +
                        "<li><strong>Flying Kings:</strong> Kings move any number of squares diagonally, " +
                        "capturing from distance.</li>" +
                        "</ul>" +
                        "<em style='color: #888; font-size: 0.8rem;'>Men can capture both forward and backward.</em>");

        panel.add(titleSpan, content);
        return panel;
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
        newGameBtn = new Button("New Game", e -> {
            if (!gameService.isMatchOver()) {
                gameService.newGame();
                clearSelection();
                refreshBoard();
            }
        });
        newGameBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        undoBtn = new Button("Undo", e -> {
            if (gameService.undo()) {
                clearSelection();
                refreshBoard();
            }
        });
        undoBtn.setEnabled(false);

        newMatchBtn = new Button("New Match", e -> {
            gameService.resetMatch();
            clearSelection();
            refreshBoard();
        });
        newMatchBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        newMatchBtn.setVisible(false);

        return new HorizontalLayout(newGameBtn, undoBtn, newMatchBtn);
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

        // Update match score display
        updateMatchDisplay();

        // Update status
        updateStatus();

        // Update button states
        updateButtonStates();
    }

    private void updateMatchDisplay() {
        MatchScore score = gameService.getMatchScore();

        // Update score label
        scoreLabel.setText(score.getScoreDisplay());

        // Update game count
        gameCountLabel.setText(score.getGameCountDisplay());

        // Update match result
        if (score.isMatchOver()) {
            matchResultLabel.setText(score.getMatchResultMessage());
            matchResultLabel.setVisible(true);

            // Style based on winner
            matchResultLabel.removeClassName("match-result-white");
            matchResultLabel.removeClassName("match-result-black");

            Player winner = score.getMatchWinner();
            if (winner == Player.WHITE) {
                matchResultLabel.addClassName("match-result-white");
            } else if (winner == Player.BLACK) {
                matchResultLabel.addClassName("match-result-black");
            }
        } else {
            matchResultLabel.setVisible(false);
        }
    }

    private void updateButtonStates() {
        // Undo button
        undoBtn.setEnabled(gameService.canUndo());

        // New Game button - disabled when match is over
        newGameBtn.setEnabled(!gameService.isMatchOver());

        // New Match button - visible when match is over
        newMatchBtn.setVisible(gameService.isMatchOver());
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
