package com.dame.ui;

import com.dame.dto.LobbyUpdate;
import com.dame.dto.OnlinePlayerDTO;
import com.dame.dto.SpectateGameDTO;
import com.dame.entity.ChallengeStatus;
import com.dame.entity.GameChallenge;
import com.dame.entity.OnlineGameSession;
import com.dame.entity.OnlineGameStatus;
import com.dame.entity.Player;
import com.dame.service.*;
import com.dame.service.broadcast.LobbyBroadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import java.util.Timer;
import java.util.TimerTask;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Optional;

@Route(value = "lobby", layout = MainLayout.class)
@PageTitle("Online Lobby | Checkers")
@PermitAll
public class LobbyView extends VerticalLayout {

    private final PlayerService playerService;
    private final OnlinePresenceService presenceService;
    private final ChallengeService challengeService;
    private final MatchmakingService matchmakingService;
    private final OnlineGameService gameService;
    private final LobbyBroadcaster lobbyBroadcaster;

    private Grid<OnlinePlayerDTO> playersGrid;
    private Grid<SpectateGameDTO> gamesGrid;
    private Div challengesPanel;
    private Button findMatchBtn;
    private Span queueStatus;
    private Registration broadcastRegistration;
    private Timer heartbeatTimer;

    private Player currentPlayer;

    public LobbyView(PlayerService playerService,
            OnlinePresenceService presenceService,
            ChallengeService challengeService,
            MatchmakingService matchmakingService,
            OnlineGameService gameService,
            LobbyBroadcaster lobbyBroadcaster) {
        this.playerService = playerService;
        this.presenceService = presenceService;
        this.challengeService = challengeService;
        this.matchmakingService = matchmakingService;
        this.gameService = gameService;
        this.lobbyBroadcaster = lobbyBroadcaster;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("lobby-view");

        // Check for authenticated player
        Optional<Player> optPlayer = playerService.getCurrentPlayer();
        if (optPlayer.isEmpty()) {
            add(new Span("Please log in to access the online lobby."));
            return;
        }
        currentPlayer = optPlayer.get();

        // Check if player has active games
        checkActiveGames();

        buildUI();
    }

    private void checkActiveGames() {
        List<OnlineGameSession> activeSessions = gameService.getActiveSessionsForPlayer(currentPlayer);
        if (!activeSessions.isEmpty()) {
            OnlineGameSession session = activeSessions.get(0);
            if (session.getStatus() == OnlineGameStatus.IN_PROGRESS) {
                // Redirect to active game
                getUI().ifPresent(ui -> ui.navigate("online-game/" + session.getId()));
            }
        }
    }

    private void buildUI() {
        // Header
        H2 title = new H2("Online Lobby");
        title.addClassName("lobby-title");

        // Main content layout
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setSizeFull();
        mainContent.setSpacing(true);

        // Left panel - Players online
        VerticalLayout playersPanel = createPlayersPanel();
        playersPanel.setWidth("40%");

        // Right panel - Challenges + Games to spectate
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("60%");
        rightPanel.setSpacing(true);
        rightPanel.setPadding(false);

        // Matchmaking section
        HorizontalLayout matchmakingSection = createMatchmakingSection();

        // Pending challenges
        challengesPanel = createChallengesPanel();

        // Games to spectate
        VerticalLayout gamesPanel = createGamesPanel();

        rightPanel.add(matchmakingSection, challengesPanel, gamesPanel);

        mainContent.add(playersPanel, rightPanel);

        add(title, mainContent);

        // Note: Initial data load moved to onAttach() to fix race condition
    }

    private VerticalLayout createPlayersPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.addClassName("lobby-panel");
        panel.setPadding(true);
        panel.setSpacing(true);

        H3 header = new H3("Players Online");

        playersGrid = new Grid<>();
        playersGrid.addColumn(OnlinePlayerDTO::getUsername).setHeader("Player").setFlexGrow(1);
        playersGrid.addColumn(OnlinePlayerDTO::getStatus).setHeader("Status").setWidth("120px");
        playersGrid.addColumn(dto -> dto.getWins() + " - " + dto.getLosses())
                .setHeader("W/L").setWidth("80px");
        playersGrid.addComponentColumn(this::createChallengeButton)
                .setHeader("").setWidth("120px");

        playersGrid.setHeight("300px");

        panel.add(header, playersGrid);
        return panel;
    }

    private Button createChallengeButton(OnlinePlayerDTO player) {
        if (player.getId().equals(currentPlayer.getId())) {
            return null; // Don't show button for self
        }

        if (player.isInGame()) {
            Button spectateBtn = new Button("Watch");
            spectateBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            // Could link to spectate their game
            return spectateBtn;
        }

        Button challengeBtn = new Button("Challenge");
        challengeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        challengeBtn.addClickListener(e -> showChallengeDialog(player));
        return challengeBtn;
    }

    private void showChallengeDialog(OnlinePlayerDTO player) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Challenge " + player.getUsername());

        TextField messageField = new TextField("Message (optional)");
        messageField.setWidth("100%");
        messageField.setPlaceholder("Let's play!");

        Button sendBtn = new Button("Send Challenge", e -> {
            try {
                Optional<Player> opponent = playerService.findById(player.getId());
                if (opponent.isPresent()) {
                    challengeService.createChallenge(currentPlayer, opponent.get(),
                            messageField.getValue());
                    Notification.show("Challenge sent to " + player.getUsername())
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            } catch (Exception ex) {
                Notification.show(ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            dialog.close();
        });
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.add(messageField);
        dialog.getFooter().add(cancelBtn, sendBtn);
        dialog.open();
    }

    private HorizontalLayout createMatchmakingSection() {
        HorizontalLayout section = new HorizontalLayout();
        section.addClassName("matchmaking-section");
        section.setAlignItems(FlexComponent.Alignment.CENTER);
        section.setSpacing(true);

        findMatchBtn = new Button("Find Match");
        findMatchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        findMatchBtn.addClickListener(e -> toggleMatchmaking());

        queueStatus = new Span();
        queueStatus.addClassName("queue-status");

        updateMatchmakingUI();

        section.add(findMatchBtn, queueStatus);
        return section;
    }

    private void toggleMatchmaking() {
        if (matchmakingService.isInQueue(currentPlayer)) {
            matchmakingService.leaveQueue(currentPlayer);
            Notification.show("Left matchmaking queue");
        } else {
            Optional<OnlineGameSession> match = matchmakingService.joinQueue(currentPlayer);
            if (match.isPresent()) {
                // Match found immediately
                Notification.show("Match found!")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("online-game/" + match.get().getId()));
            } else {
                Notification.show("Searching for opponent...");
            }
        }
        updateMatchmakingUI();
    }

    private void updateMatchmakingUI() {
        boolean inQueue = matchmakingService.isInQueue(currentPlayer);
        findMatchBtn.setText(inQueue ? "Cancel Search" : "Find Match");
        findMatchBtn.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        findMatchBtn.addThemeVariants(inQueue ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_PRIMARY);

        long queueSize = matchmakingService.getQueueSize();
        queueStatus.setText(inQueue ? "Searching... (" + queueSize + " in queue)" : "");
    }

    private Div createChallengesPanel() {
        Div panel = new Div();
        panel.addClassName("challenges-panel");
        return panel;
    }

    private void refreshChallengesPanel() {
        challengesPanel.removeAll();

        List<GameChallenge> received = challengeService.getPendingChallengesReceived(currentPlayer);

        if (received.isEmpty()) {
            challengesPanel.add(new Span("No pending challenges"));
            return;
        }

        H3 header = new H3("Incoming Challenges");
        challengesPanel.add(header);

        for (GameChallenge challenge : received) {
            HorizontalLayout row = new HorizontalLayout();
            row.addClassName("challenge-row");
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(true);

            Span info = new Span(challenge.getChallenger().getUsername() + " challenges you!");
            if (challenge.getMessage() != null && !challenge.getMessage().isBlank()) {
                info.setText(info.getText() + " \"" + challenge.getMessage() + "\"");
            }

            Button acceptBtn = new Button("Accept", e -> acceptChallenge(challenge));
            acceptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);

            Button declineBtn = new Button("Decline", e -> declineChallenge(challenge));
            declineBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            row.add(info, acceptBtn, declineBtn);
            challengesPanel.add(row);
        }
    }

    private void acceptChallenge(GameChallenge challenge) {
        try {
            OnlineGameSession session = challengeService.acceptChallenge(
                    challenge.getId(), currentPlayer);
            Notification.show("Game started!")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            getUI().ifPresent(ui -> ui.navigate("online-game/" + session.getId()));
        } catch (Exception e) {
            Notification.show(e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            refreshChallengesPanel();
        }
    }

    private void declineChallenge(GameChallenge challenge) {
        challengeService.declineChallenge(challenge.getId(), currentPlayer);
        Notification.show("Challenge declined");
        refreshChallengesPanel();
    }

    private VerticalLayout createGamesPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.addClassName("lobby-panel");
        panel.setPadding(true);
        panel.setSpacing(true);

        H3 header = new H3("Games to Watch");

        gamesGrid = new Grid<>();
        gamesGrid.addColumn(SpectateGameDTO::getMatchupDisplay).setHeader("Match").setFlexGrow(1);
        gamesGrid.addColumn(SpectateGameDTO::getScoreDisplay).setHeader("Score").setWidth("80px");
        gamesGrid.addColumn(dto -> dto.getSpectatorCount() + " watching")
                .setHeader("Viewers").setWidth("100px");
        gamesGrid.addComponentColumn(this::createWatchButton)
                .setHeader("").setWidth("100px");

        gamesGrid.setHeight("200px");

        panel.add(header, gamesGrid);
        return panel;
    }

    private Button createWatchButton(SpectateGameDTO game) {
        Button btn = new Button("Watch");
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.addClickListener(
                e -> getUI().ifPresent(ui -> ui.navigate("online-game/" + game.getSessionId() + "?spectate=true")));
        return btn;
    }

    private void refreshData() {
        // Refresh players list
        List<OnlinePlayerDTO> players = presenceService.getOnlinePlayersExcept(currentPlayer.getId());
        playersGrid.setItems(players);

        // Refresh challenges
        refreshChallengesPanel();

        // Refresh spectatable games
        List<OnlineGameSession> sessions = gameService.getSpectatableGames();
        List<SpectateGameDTO> games = sessions.stream()
                .map(s -> SpectateGameDTO.fromSession(s, 0)) // TODO: get actual spectator count
                .toList();
        gamesGrid.setItems(games);

        // Update matchmaking
        updateMatchmakingUI();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Mark player as online FIRST, before loading data
        if (currentPlayer != null) {
            presenceService.playerOnline(currentPlayer);
        }

        // Register for lobby updates
        UI ui = attachEvent.getUI();
        broadcastRegistration = lobbyBroadcaster.register(update -> {
            ui.access(() -> handleLobbyUpdate(update));
        });

        // Initial data load AFTER registering online (fixes race condition)
        refreshData();

        // Start heartbeat timer to keep presence alive (30 second interval)
        if (currentPlayer != null) {
            heartbeatTimer = new Timer(true); // daemon thread
            heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        presenceService.heartbeat(currentPlayer);
                        // Also refresh data periodically to catch any missed updates
                        ui.access(() -> refreshData());
                    } catch (Exception e) {
                        // UI might be detached, timer will be cancelled in onDetach
                    }
                }
            }, 30000, 30000); // 30 seconds interval
        }

        // Check for pending match
        if (currentPlayer != null) {
            Optional<OnlineGameSession> match = matchmakingService.checkForMatch(currentPlayer);
            match.ifPresent(session -> {
                ui.navigate("online-game/" + session.getId());
            });
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        // Cancel heartbeat timer
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }

        // Unregister from broadcasts
        if (broadcastRegistration != null) {
            broadcastRegistration.remove();
        }

        // Mark player as offline
        if (currentPlayer != null) {
            presenceService.playerOffline(currentPlayer);
        }
    }

    private void handleLobbyUpdate(LobbyUpdate update) {
        switch (update.getType()) {
            case PLAYER_ONLINE, PLAYER_OFFLINE, FULL_REFRESH -> {
                refreshData();
            }
            case CHALLENGE_RECEIVED -> {
                // Check if this challenge is for us
                if (currentPlayer != null) {
                    refreshChallengesPanel();
                    Notification.show("New challenge from " + update.getPlayerUsername())
                            .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
            }
            case CHALLENGE_CANCELLED, CHALLENGE_EXPIRED -> {
                refreshChallengesPanel();
            }
            case MATCHMAKING_FOUND -> {
                // Check if we're matched
                Optional<OnlineGameSession> match = matchmakingService.checkForMatch(currentPlayer);
                match.ifPresent(session -> {
                    Notification.show("Match found!")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    getUI().ifPresent(ui -> ui.navigate("online-game/" + session.getId()));
                });
            }
            case GAME_STARTED -> {
                // Check if this game involves us
                if (update.getSessionId() != null) {
                    gameService.findById(update.getSessionId()).ifPresent(session -> {
                        if (session.hasPlayer(currentPlayer)) {
                            getUI().ifPresent(ui -> ui.navigate("online-game/" + session.getId()));
                        }
                    });
                }
                // Refresh spectatable games
                refreshData();
            }
            case SPECTATABLE_GAMES_CHANGED -> {
                List<OnlineGameSession> sessions = gameService.getSpectatableGames();
                List<SpectateGameDTO> games = sessions.stream()
                        .map(s -> SpectateGameDTO.fromSession(s, 0))
                        .toList();
                gamesGrid.setItems(games);
            }
            default -> {
                // Ignore other updates
            }
        }
    }
}
