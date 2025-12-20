package com.dame.entity;

import com.dame.engine.GameState;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents an online multiplayer game session.
 * Stores the game state for persistence and synchronization between players.
 */
@Entity
@Table(name = "online_game_sessions")
public class OnlineGameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique session code for sharing/joining (e.g., "GAME-ABC123")
     */
    @Column(unique = true, nullable = false)
    private String sessionCode;

    /**
     * Player controlling white pieces
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "white_player_id")
    private Player whitePlayer;

    /**
     * Player controlling black pieces
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "black_player_id")
    private Player blackPlayer;

    /**
     * Current status of the game session
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OnlineGameStatus status = OnlineGameStatus.WAITING;

    /**
     * Serialized board state (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String boardStateJson;

    /**
     * Whose turn it is (WHITE or BLACK as string)
     */
    @Column(nullable = false)
    private String currentTurn = "WHITE";

    /**
     * Current game state (IN_PROGRESS, WHITE_WINS, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameState gameState = GameState.IN_PROGRESS;

    /**
     * Position of piece in multi-jump sequence (JSON, nullable)
     */
    @Column(columnDefinition = "TEXT")
    private String multiJumpPositionJson;

    /**
     * Number of games won by white player in this session
     */
    @Column(nullable = false)
    private int whiteWins = 0;

    /**
     * Number of games won by black player in this session
     */
    @Column(nullable = false)
    private int blackWins = 0;

    /**
     * Total games played in this session (for best-of matches)
     */
    @Column(nullable = false)
    private int gamesPlayed = 0;

    /**
     * When the session was created
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * When the last move was made
     */
    private LocalDateTime lastMoveAt;

    /**
     * When the session ended
     */
    private LocalDateTime completedAt;

    /**
     * Whether white player is currently connected
     */
    @Column(nullable = false)
    private boolean whiteConnected = false;

    /**
     * Whether black player is currently connected
     */
    @Column(nullable = false)
    private boolean blackConnected = false;

    /**
     * Player who requested a rematch (nullable if no pending request)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rematch_requested_by_id")
    private Player rematchRequestedBy;

    /**
     * When the rematch was requested
     */
    private LocalDateTime rematchRequestedAt;

    public OnlineGameSession() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public Player getWhitePlayer() {
        return whitePlayer;
    }

    public void setWhitePlayer(Player whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public Player getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(Player blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public OnlineGameStatus getStatus() {
        return status;
    }

    public void setStatus(OnlineGameStatus status) {
        this.status = status;
    }

    public String getBoardStateJson() {
        return boardStateJson;
    }

    public void setBoardStateJson(String boardStateJson) {
        this.boardStateJson = boardStateJson;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public String getMultiJumpPositionJson() {
        return multiJumpPositionJson;
    }

    public void setMultiJumpPositionJson(String multiJumpPositionJson) {
        this.multiJumpPositionJson = multiJumpPositionJson;
    }

    public int getWhiteWins() {
        return whiteWins;
    }

    public void setWhiteWins(int whiteWins) {
        this.whiteWins = whiteWins;
    }

    public int getBlackWins() {
        return blackWins;
    }

    public void setBlackWins(int blackWins) {
        this.blackWins = blackWins;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastMoveAt() {
        return lastMoveAt;
    }

    public void setLastMoveAt(LocalDateTime lastMoveAt) {
        this.lastMoveAt = lastMoveAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isWhiteConnected() {
        return whiteConnected;
    }

    public void setWhiteConnected(boolean whiteConnected) {
        this.whiteConnected = whiteConnected;
    }

    public boolean isBlackConnected() {
        return blackConnected;
    }

    public void setBlackConnected(boolean blackConnected) {
        this.blackConnected = blackConnected;
    }

    public Player getRematchRequestedBy() {
        return rematchRequestedBy;
    }

    public void setRematchRequestedBy(Player rematchRequestedBy) {
        this.rematchRequestedBy = rematchRequestedBy;
    }

    public LocalDateTime getRematchRequestedAt() {
        return rematchRequestedAt;
    }

    public void setRematchRequestedAt(LocalDateTime rematchRequestedAt) {
        this.rematchRequestedAt = rematchRequestedAt;
    }

    /**
     * Checks if there is a pending rematch request.
     */
    public boolean hasPendingRematchRequest() {
        return rematchRequestedBy != null;
    }

    /**
     * Clears any pending rematch request.
     */
    public void clearRematchRequest() {
        this.rematchRequestedBy = null;
        this.rematchRequestedAt = null;
    }

    /**
     * Checks if a player is part of this game session.
     */
    public boolean hasPlayer(Player player) {
        return (whitePlayer != null && whitePlayer.getId().equals(player.getId())) ||
                (blackPlayer != null && blackPlayer.getId().equals(player.getId()));
    }

    /**
     * Gets the color assigned to a player in this session.
     */
    public com.dame.engine.Player getPlayerColor(Player player) {
        if (whitePlayer != null && whitePlayer.getId().equals(player.getId())) {
            return com.dame.engine.Player.WHITE;
        }
        if (blackPlayer != null && blackPlayer.getId().equals(player.getId())) {
            return com.dame.engine.Player.BLACK;
        }
        return null;
    }

    /**
     * Gets the opponent of a player in this session.
     */
    public Player getOpponent(Player player) {
        if (whitePlayer != null && whitePlayer.getId().equals(player.getId())) {
            return blackPlayer;
        }
        if (blackPlayer != null && blackPlayer.getId().equals(player.getId())) {
            return whitePlayer;
        }
        return null;
    }
}
