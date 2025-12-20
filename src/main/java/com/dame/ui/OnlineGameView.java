package com.dame.ui;

import com.dame.dto.ChatUpdate;
import com.dame.dto.GameUpdate;
import com.dame.dto.MoveDTO;
import com.dame.dto.MoveResult;
import com.dame.engine.*;
import com.dame.entity.ChatMessage;
import com.dame.entity.OnlineGameSession;
import com.dame.entity.OnlineGameStatus;
import com.dame.entity.Player;
import com.dame.service.ChatService;
import com.dame.service.OnlineGameService;
import com.dame.service.PlayerService;
import com.dame.service.broadcast.ChatBroadcaster;
import com.dame.service.broadcast.GameSessionBroadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route(value = "online-game", layout = MainLayout.class)
@PageTitle("Online Game | Checkers")
@PermitAll
public class OnlineGameView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PlayerService playerService;
    private final OnlineGameService gameService;
    private final ChatService chatService;
    private final GameSessionBroadcaster gameBroadcaster;
    private final ChatBroadcaster chatBroadcaster;

    private final BoardSquare[][] squares = new BoardSquare[8][8];
    private Span statusLabel;
    private Span scoreLabel;
    private Span opponentLabel;
    private Span yourColorLabel;
    private VerticalLayout chatMessages;
    private TextField chatInput;

    // Rematch UI components
    private HorizontalLayout rematchControls;
    private Button requestRematchBtn;
    private Button acceptRematchBtn;
    private Button declineRematchBtn;
    private Span rematchStatus;

    private BoardSquare selectedSquare;
    private List<Move> currentValidMoves;

    private OnlineGameSession session;
    private Player currentPlayer;
    private com.dame.engine.Player myColor;
    private boolean isSpectator;
    private GameLogic gameLogic;

    private Registration gameRegistration;
    private Registration chatRegistration;

    public OnlineGameView(PlayerService playerService,
            OnlineGameService gameService,
            ChatService chatService,
            GameSessionBroadcaster gameBroadcaster,
            ChatBroadcaster chatBroadcaster) {
        this.playerService = playerService;
        this.gameService = gameService;
        this.chatService = chatService;
        this.gameBroadcaster = gameBroadcaster;
        this.chatBroadcaster = chatBroadcaster;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("online-game-view");
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long sessionId) {
        // Check for spectate mode
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        isSpectator = params.containsKey("spectate");

        // Get current player
        Optional<Player> optPlayer = playerService.getCurrentPlayer();
        if (optPlayer.isEmpty()) {
            event.rerouteTo("lobby");
            return;
        }
        currentPlayer = optPlayer.get();

        if (sessionId == null) {
            event.rerouteTo("lobby");
            return;
        }

        // Load session
        Optional<OnlineGameSession> optSession = gameService.findById(sessionId);
        if (optSession.isEmpty()) {
            Notification.show("Game session not found")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            event.rerouteTo("lobby");
            return;
        }

        session = optSession.get();

        // Determine if player is participant or spectator
        myColor = session.getPlayerColor(currentPlayer);
        if (myColor == null) {
            isSpectator = true;
        }

        // Reconstruct game state
        gameLogic = gameService.reconstructGame(session);

        buildUI();
        refreshBoard();
    }

    private void buildUI() {
        removeAll();

        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H2 title = new H2(isSpectator ? "Spectating" : "Online Game");

        Button backBtn = new Button("Back to Lobby", e -> getUI().ifPresent(ui -> ui.navigate("lobby")));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        header.add(title, backBtn);

        // Main content
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setSizeFull();
        mainContent.setSpacing(true);

        // Left side - Game info and board
        VerticalLayout gamePanel = createGamePanel();
        gamePanel.setWidth("60%");

        // Right side - Chat
        VerticalLayout chatPanel = createChatPanel();
        chatPanel.setWidth("40%");

        mainContent.add(gamePanel, chatPanel);

        add(header, mainContent);
    }

    private VerticalLayout createGamePanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setAlignItems(FlexComponent.Alignment.CENTER);
        panel.setSpacing(true);
        panel.setPadding(false);

        // Player info
        HorizontalLayout playerInfo = new HorizontalLayout();
        playerInfo.setAlignItems(FlexComponent.Alignment.CENTER);
        playerInfo.setSpacing(true);

        String whiteName = session.getWhitePlayer() != null ? session.getWhitePlayer().getUsername() : "?";
        String blackName = session.getBlackPlayer() != null ? session.getBlackPlayer().getUsername() : "?";

        opponentLabel = new Span(whiteName + " vs " + blackName);
        opponentLabel.addClassName("opponent-label");

        yourColorLabel = new Span();
        if (!isSpectator) {
            yourColorLabel.setText("You are " + myColor.name());
            yourColorLabel.addClassName(myColor == com.dame.engine.Player.WHITE ? "color-white" : "color-black");
        } else {
            yourColorLabel.setText("Spectating");
        }

        playerInfo.add(opponentLabel, yourColorLabel);

        // Score
        scoreLabel = new Span();
        scoreLabel.addClassName("score-label");

        // Status
        statusLabel = new Span();
        statusLabel.addClassName("status-label");

        // Board
        Div boardContainer = createBoardGrid();

        // Controls (only for players)
        HorizontalLayout controls = new HorizontalLayout();
        controls.setAlignItems(FlexComponent.Alignment.CENTER);
        controls.setSpacing(true);

        if (!isSpectator) {
            Button forfeitBtn = new Button("Forfeit Round", e -> forfeitRound());
            forfeitBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            controls.add(forfeitBtn);
        }

        // Rematch controls (shown after game ends)
        rematchControls = new HorizontalLayout();
        rematchControls.setAlignItems(FlexComponent.Alignment.CENTER);
        rematchControls.setSpacing(true);
        rematchControls.setVisible(false);

        if (!isSpectator) {
            requestRematchBtn = new Button("Request Rematch", e -> requestRematch());
            requestRematchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

            acceptRematchBtn = new Button("Accept Rematch", e -> acceptRematch());
            acceptRematchBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);

            declineRematchBtn = new Button("Decline", e -> declineRematch());
            declineRematchBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            rematchStatus = new Span();
            rematchStatus.addClassName("rematch-status");

            rematchControls.add(requestRematchBtn, acceptRematchBtn, declineRematchBtn, rematchStatus);
        }

        panel.add(playerInfo, scoreLabel, statusLabel, boardContainer, controls, rematchControls);
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
                if (!isSpectator) {
                    square.addClickListener(e -> handleSquareClick(square));
                }
                squares[row][col] = square;
                grid.add(square);
            }
        }

        boardContainer.add(grid);
        return boardContainer;
    }

    private VerticalLayout createChatPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.addClassName("chat-panel");
        panel.setSizeFull();
        panel.setPadding(true);
        panel.setSpacing(true);

        H2 chatTitle = new H2("Chat");
        chatTitle.getStyle().set("margin", "0");

        // Messages area
        chatMessages = new VerticalLayout();
        chatMessages.addClassName("chat-messages");
        chatMessages.setPadding(false);
        chatMessages.setSpacing(false);

        Scroller scroller = new Scroller(chatMessages);
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.setSizeFull();
        scroller.getStyle().set("flex-grow", "1");

        // Input area
        HorizontalLayout inputArea = new HorizontalLayout();
        inputArea.setWidthFull();

        chatInput = new TextField();
        chatInput.setPlaceholder("Type a message...");
        chatInput.setWidthFull();
        chatInput.addKeyPressListener(Key.ENTER, e -> sendMessage());

        Button sendBtn = new Button("Send", e -> sendMessage());
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        inputArea.add(chatInput, sendBtn);
        inputArea.expand(chatInput);

        panel.add(chatTitle, scroller, inputArea);
        panel.expand(scroller);

        // Load existing messages
        loadChatHistory();

        return panel;
    }

    private void loadChatHistory() {
        List<ChatMessage> messages = chatService.getMessages(session.getId());
        for (ChatMessage msg : messages) {
            addChatMessage(msg);
        }
    }

    private void addChatMessage(ChatMessage message) {
        Div msgDiv = new Div();
        msgDiv.addClassName("chat-message");

        if (message.isSystemMessage()) {
            msgDiv.addClassName("system-message");
            msgDiv.setText(message.getContent());
        } else {
            String sender = message.getSender().getUsername();
            String time = message.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm"));

            Span senderSpan = new Span(sender + ": ");
            senderSpan.addClassName("sender-name");
            if (message.getSender().getId().equals(currentPlayer.getId())) {
                senderSpan.addClassName("own-message");
            }

            Span contentSpan = new Span(message.getContent());
            Span timeSpan = new Span(" " + time);
            timeSpan.addClassName("message-time");

            msgDiv.add(senderSpan, contentSpan, timeSpan);
        }

        chatMessages.add(msgDiv);

        // Scroll to bottom
        msgDiv.getElement().executeJs("this.scrollIntoView()");
    }

    private void addChatUpdate(ChatUpdate update) {
        Div msgDiv = new Div();
        msgDiv.addClassName("chat-message");

        if (update.isSystemMessage()) {
            msgDiv.addClassName("system-message");
            msgDiv.setText(update.getContent());
        } else {
            String time = update.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm"));

            Span senderSpan = new Span(update.getSenderUsername() + ": ");
            senderSpan.addClassName("sender-name");
            if (update.getSenderId().equals(currentPlayer.getId())) {
                senderSpan.addClassName("own-message");
            }

            Span contentSpan = new Span(update.getContent());
            Span timeSpan = new Span(" " + time);
            timeSpan.addClassName("message-time");

            msgDiv.add(senderSpan, contentSpan, timeSpan);
        }

        chatMessages.add(msgDiv);
        msgDiv.getElement().executeJs("this.scrollIntoView()");
    }

    private void sendMessage() {
        String content = chatInput.getValue();
        if (content == null || content.isBlank()) {
            return;
        }

        try {
            chatService.sendMessage(session.getId(), currentPlayer, content);
            chatInput.clear();
        } catch (Exception e) {
            Notification.show("Failed to send message")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void handleSquareClick(BoardSquare square) {
        if (isSpectator) {
            return;
        }

        // Check if it's my turn
        com.dame.engine.Player currentTurn = com.dame.engine.Player.valueOf(session.getCurrentTurn());
        if (currentTurn != myColor) {
            Notification.show("It's not your turn");
            return;
        }

        if (gameLogic.isGameOver()) {
            return;
        }

        int row = square.getRow();
        int col = square.getCol();

        if (selectedSquare == null) {
            // Try to select
            if (canSelect(row, col)) {
                selectSquare(square);
            }
        } else {
            if (square == selectedSquare) {
                clearSelection();
            } else if (square.isHighlighted()) {
                executeMove(row, col);
            } else if (canSelect(row, col)) {
                clearSelection();
                selectSquare(square);
            } else {
                clearSelection();
            }
        }
    }

    private boolean canSelect(int row, int col) {
        Piece piece = gameLogic.getBoard().get(row, col);
        if (piece == null || piece.getOwner() != myColor) {
            return false;
        }

        // Check if in multi-jump
        if (gameLogic.isInMultiJump()) {
            Position jumpPos = gameLogic.getMultiJumpPosition();
            return jumpPos.row() == row && jumpPos.col() == col;
        }

        return !gameLogic.getValidMovesFor(row, col).isEmpty();
    }

    private void selectSquare(BoardSquare square) {
        selectedSquare = square;
        square.setSelected(true);

        currentValidMoves = gameLogic.getValidMovesFor(square.getRow(), square.getCol());
        for (Move move : currentValidMoves) {
            squares[move.getEndRow()][move.getEndCol()].setHighlighted(true);
        }
    }

    private void executeMove(int endRow, int endCol) {
        Move selectedMove = currentValidMoves.stream()
                .filter(m -> m.getEndRow() == endRow && m.getEndCol() == endCol)
                .findFirst()
                .orElse(null);

        if (selectedMove == null) {
            return;
        }

        // Create move DTO
        MoveDTO moveDto = MoveDTO.fromMove(selectedMove);

        // Apply move via service
        MoveResult result = gameService.applyMove(session.getId(), currentPlayer, moveDto);

        if (!result.isSuccess()) {
            Notification.show(result.getErrorMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            clearSelection();
            return;
        }

        // Reload session and game state
        session = gameService.findById(session.getId()).orElse(session);
        gameLogic = gameService.reconstructGame(session);

        clearSelection();
        refreshBoard();

        // Auto-select if multi-jump continues
        if (!result.isTurnEnded() && gameLogic.isInMultiJump()) {
            Position jumpPos = gameLogic.getMultiJumpPosition();
            selectSquare(squares[jumpPos.row()][jumpPos.col()]);
        }
    }

    private void clearSelection() {
        if (selectedSquare != null) {
            selectedSquare.setSelected(false);
            selectedSquare = null;
        }

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares[r][c].clearHighlights();
            }
        }

        currentValidMoves = null;
    }

    private void refreshBoard() {
        Board board = gameLogic.getBoard();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.get(row, col);
                squares[row][col].setPiece(piece);
            }
        }

        updateScore();
        updateStatus();
        updateRematchUI();
    }

    private void updateScore() {
        scoreLabel.setText("Score: " + session.getWhiteWins() + " - " + session.getBlackWins() +
                " (Game " + (session.getGamesPlayed() + 1) + ")");
    }

    private void updateStatus() {
        GameState state = gameLogic.getGameState();

        if (state == GameState.IN_PROGRESS) {
            com.dame.engine.Player turn = com.dame.engine.Player.valueOf(session.getCurrentTurn());
            String turnName = turn == com.dame.engine.Player.WHITE ? session.getWhitePlayer().getUsername()
                    : session.getBlackPlayer().getUsername();

            if (gameLogic.isInMultiJump()) {
                statusLabel.setText(turnName + " must continue jumping");
            } else {
                statusLabel.setText(turnName + "'s turn");
            }

            if (!isSpectator && turn == myColor) {
                statusLabel.addClassName("your-turn");
            } else {
                statusLabel.removeClassName("your-turn");
            }
        } else {
            String winner = state == GameState.WHITE_WINS ? session.getWhitePlayer().getUsername()
                    : session.getBlackPlayer().getUsername();
            statusLabel.setText(winner + " wins this round!");
            statusLabel.addClassName("game-over");
        }
    }

    private void forfeitRound() {
        gameService.forfeitRound(session.getId(), currentPlayer);
        session = gameService.findById(session.getId()).orElse(session);
        gameLogic = gameService.reconstructGame(session);
        refreshBoard();
    }

    private void startNewRound() {
        gameService.startNewRound(session.getId());
        session = gameService.findById(session.getId()).orElse(session);
        gameLogic = gameService.reconstructGame(session);
        refreshBoard();
    }

    private void requestRematch() {
        gameService.requestRematch(session.getId(), currentPlayer);
        session = gameService.findById(session.getId()).orElse(session);
        updateRematchUI();
    }

    private void acceptRematch() {
        gameService.acceptRematch(session.getId(), currentPlayer);
        session = gameService.findById(session.getId()).orElse(session);
        gameLogic = gameService.reconstructGame(session);
        refreshBoard();
    }

    private void declineRematch() {
        gameService.declineRematch(session.getId(), currentPlayer);
        session = gameService.findById(session.getId()).orElse(session);
        updateRematchUI();
        Notification.show("Rematch declined");
    }

    private void updateRematchUI() {
        if (isSpectator || rematchControls == null) {
            return;
        }

        boolean gameOver = gameLogic.isGameOver();
        rematchControls.setVisible(gameOver);

        if (!gameOver) {
            return;
        }

        boolean hasPendingRequest = session.hasPendingRematchRequest();
        Player requestedBy = session.getRematchRequestedBy();
        boolean iRequestedIt = hasPendingRequest && requestedBy != null
                && requestedBy.getId().equals(currentPlayer.getId());

        if (!hasPendingRequest) {
            // No pending request - show "Request Rematch" button
            requestRematchBtn.setVisible(true);
            acceptRematchBtn.setVisible(false);
            declineRematchBtn.setVisible(false);
            rematchStatus.setText("");
        } else if (iRequestedIt) {
            // I requested - show waiting message
            requestRematchBtn.setVisible(false);
            acceptRematchBtn.setVisible(false);
            declineRematchBtn.setVisible(false);
            rematchStatus.setText("Waiting for opponent...");
        } else {
            // Opponent requested - show accept/decline
            requestRematchBtn.setVisible(false);
            acceptRematchBtn.setVisible(true);
            declineRematchBtn.setVisible(true);
            rematchStatus.setText(requestedBy.getUsername() + " wants a rematch!");
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        if (session == null) {
            return;
        }

        UI ui = attachEvent.getUI();

        // Mark player as connected
        if (!isSpectator) {
            gameService.setPlayerConnected(session.getId(), currentPlayer, true);
        }

        // Register for game updates
        gameRegistration = gameBroadcaster.register(session.getId(), update -> {
            ui.access(() -> handleGameUpdate(update));
        });

        // Register for chat updates
        chatRegistration = chatBroadcaster.register(session.getId(), update -> {
            ui.access(() -> addChatUpdate(update));
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        // Unregister from broadcasts
        if (gameRegistration != null) {
            gameRegistration.remove();
        }
        if (chatRegistration != null) {
            chatRegistration.remove();
        }

        // Mark player as disconnected
        if (!isSpectator && session != null) {
            gameService.setPlayerConnected(session.getId(), currentPlayer, false);
        }
    }

    private void handleGameUpdate(GameUpdate update) {
        // Reload session from database
        session = gameService.findById(session.getId()).orElse(session);
        gameLogic = gameService.reconstructGame(session);

        clearSelection();
        refreshBoard();

        // Show notification for significant events
        switch (update.getType()) {
            case MOVE_MADE -> {
                // No notification needed, board updates visually
            }
            case PLAYER_CONNECTED -> {
                if (update.getMessage() != null) {
                    Notification.show(update.getMessage());
                }
            }
            case PLAYER_DISCONNECTED -> {
                if (update.getMessage() != null) {
                    Notification.show(update.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                }
            }
            case PLAYER_FORFEITED, GAME_ENDED -> {
                if (update.getMessage() != null) {
                    Notification.show(update.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
            }
            case NEW_ROUND -> {
                Notification.show("New round started!")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            case REMATCH_REQUESTED -> {
                updateRematchUI();
                if (update.getMessage() != null) {
                    Notification.show(update.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
            }
            case REMATCH_DECLINED -> {
                updateRematchUI();
                if (update.getMessage() != null) {
                    Notification.show(update.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                }
            }
            default -> {
            }
        }
    }
}
