package com.dame.service;

import com.dame.dto.GameUpdate;
import com.dame.dto.MoveDTO;
import com.dame.dto.MoveResult;
import com.dame.engine.*;
import com.dame.entity.OnlineGameSession;
import com.dame.entity.OnlineGameStatus;
import com.dame.entity.Player;
import com.dame.repository.OnlineGameSessionRepository;
import com.dame.service.broadcast.GameSessionBroadcaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core service for online multiplayer game logic.
 */
@Service
public class OnlineGameService {

    private final OnlineGameSessionRepository sessionRepository;
    private final GameSessionBroadcaster broadcaster;

    public OnlineGameService(OnlineGameSessionRepository sessionRepository,
            GameSessionBroadcaster broadcaster) {
        this.sessionRepository = sessionRepository;
        this.broadcaster = broadcaster;
    }

    /**
     * Create a new game session.
     */
    @Transactional
    public OnlineGameSession createSession(Player whitePlayer, Player blackPlayer) {
        OnlineGameSession session = new OnlineGameSession();
        session.setSessionCode(generateSessionCode());
        session.setWhitePlayer(whitePlayer);
        session.setBlackPlayer(blackPlayer);
        session.setStatus(OnlineGameStatus.IN_PROGRESS);
        session.setCurrentTurn("WHITE");
        session.setGameState(GameState.IN_PROGRESS);

        // Initialize board
        Board board = new Board();
        board.setupInitialPosition();
        session.setBoardStateJson(BoardStateSerializer.serialize(board));

        return sessionRepository.save(session);
    }

    /**
     * Find session by ID.
     */
    public Optional<OnlineGameSession> findById(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }

    /**
     * Find session by code.
     */
    public Optional<OnlineGameSession> findByCode(String sessionCode) {
        return sessionRepository.findBySessionCode(sessionCode);
    }

    /**
     * Get active sessions for a player.
     */
    public List<OnlineGameSession> getActiveSessionsForPlayer(Player player) {
        return sessionRepository.findByPlayerAndStatusIn(player,
                List.of(OnlineGameStatus.WAITING, OnlineGameStatus.IN_PROGRESS));
    }

    /**
     * Apply a move to an online game.
     */
    @Transactional
    public MoveResult applyMove(Long sessionId, Player player, MoveDTO moveDto) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return MoveResult.failure("Game session not found");
        }

        OnlineGameSession session = optSession.get();

        // Validate it's this player's turn
        com.dame.engine.Player currentTurn = com.dame.engine.Player.valueOf(session.getCurrentTurn());
        com.dame.engine.Player playerColor = session.getPlayerColor(player);

        if (playerColor == null) {
            return MoveResult.failure("You are not a player in this game");
        }

        if (playerColor != currentTurn) {
            return MoveResult.failure("It's not your turn");
        }

        if (session.getStatus() != OnlineGameStatus.IN_PROGRESS) {
            return MoveResult.failure("Game is not in progress");
        }

        // Reconstruct game state
        GameLogic game = reconstructGame(session);

        // Apply the move
        Move move = moveDto.toMove();
        boolean turnEnded = game.applyMove(move);

        if (!turnEnded && !game.isInMultiJump()) {
            return MoveResult.failure("Invalid move");
        }

        // Update session state
        session.setBoardStateJson(BoardStateSerializer.serialize(game.getBoard()));
        session.setCurrentTurn(game.getCurrentPlayer().name());
        session.setGameState(game.getGameState());
        session.setMultiJumpPositionJson(
                game.isInMultiJump()
                        ? BoardStateSerializer.serializePosition(game.getMultiJumpPosition())
                        : null);
        session.setLastMoveAt(LocalDateTime.now());

        // Check if game ended
        if (game.isGameOver()) {
            handleGameEnd(session, game.getGameState());
        }

        sessionRepository.save(session);

        // Broadcast update
        GameUpdate update = GameUpdate.builder(GameUpdate.UpdateType.MOVE_MADE, sessionId)
                .boardStateJson(session.getBoardStateJson())
                .currentTurn(session.getCurrentTurn())
                .gameState(session.getGameState())
                .sessionStatus(session.getStatus())
                .multiJumpPositionJson(session.getMultiJumpPositionJson())
                .whiteWins(session.getWhiteWins())
                .blackWins(session.getBlackWins())
                .gamesPlayed(session.getGamesPlayed())
                .lastMove(moveDto)
                .build();

        broadcaster.broadcast(sessionId, update);

        return MoveResult.success(turnEnded, game.getGameState(), session.getMultiJumpPositionJson());
    }

    /**
     * Player forfeits the current round.
     */
    @Transactional
    public void forfeitRound(Long sessionId, Player player) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return;
        }

        OnlineGameSession session = optSession.get();
        com.dame.engine.Player playerColor = session.getPlayerColor(player);

        if (playerColor == null || session.getStatus() != OnlineGameStatus.IN_PROGRESS) {
            return;
        }

        // Opponent wins this round
        GameState result = playerColor == com.dame.engine.Player.WHITE
                ? GameState.BLACK_WINS
                : GameState.WHITE_WINS;

        handleGameEnd(session, result);
        session.setGameState(result);
        sessionRepository.save(session);

        // Broadcast update
        GameUpdate update = GameUpdate.builder(GameUpdate.UpdateType.PLAYER_FORFEITED, sessionId)
                .gameState(result)
                .sessionStatus(session.getStatus())
                .whiteWins(session.getWhiteWins())
                .blackWins(session.getBlackWins())
                .gamesPlayed(session.getGamesPlayed())
                .message(player.getUsername() + " forfeited")
                .build();

        broadcaster.broadcast(sessionId, update);
    }

    /**
     * Start a new round in the same session.
     */
    @Transactional
    public void startNewRound(Long sessionId) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return;
        }

        OnlineGameSession session = optSession.get();

        // Reset board
        Board board = new Board();
        board.setupInitialPosition();
        session.setBoardStateJson(BoardStateSerializer.serialize(board));
        session.setCurrentTurn("WHITE");
        session.setGameState(GameState.IN_PROGRESS);
        session.setMultiJumpPositionJson(null);
        session.setStatus(OnlineGameStatus.IN_PROGRESS);

        sessionRepository.save(session);

        // Broadcast update
        GameUpdate update = GameUpdate.builder(GameUpdate.UpdateType.NEW_ROUND, sessionId)
                .boardStateJson(session.getBoardStateJson())
                .currentTurn("WHITE")
                .gameState(GameState.IN_PROGRESS)
                .sessionStatus(OnlineGameStatus.IN_PROGRESS)
                .whiteWins(session.getWhiteWins())
                .blackWins(session.getBlackWins())
                .gamesPlayed(session.getGamesPlayed())
                .message("New round started")
                .build();

        broadcaster.broadcast(sessionId, update);
    }

    /**
     * Mark player as connected/disconnected.
     */
    @Transactional
    public void setPlayerConnected(Long sessionId, Player player, boolean connected) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return;
        }

        OnlineGameSession session = optSession.get();
        com.dame.engine.Player playerColor = session.getPlayerColor(player);

        if (playerColor == null) {
            return;
        }

        if (playerColor == com.dame.engine.Player.WHITE) {
            session.setWhiteConnected(connected);
        } else {
            session.setBlackConnected(connected);
        }

        sessionRepository.save(session);

        GameUpdate.UpdateType type = connected
                ? GameUpdate.UpdateType.PLAYER_CONNECTED
                : GameUpdate.UpdateType.PLAYER_DISCONNECTED;

        GameUpdate update = GameUpdate.builder(type, sessionId)
                .message(player.getUsername() + (connected ? " connected" : " disconnected"))
                .build();

        broadcaster.broadcast(sessionId, update);
    }

    /**
     * Get games available for spectating.
     */
    public List<OnlineGameSession> getSpectatableGames() {
        return sessionRepository.findSpectatable(OnlineGameStatus.IN_PROGRESS);
    }

    /**
     * Reconstruct GameLogic from session state.
     */
    public GameLogic reconstructGame(OnlineGameSession session) {
        GameLogic game = new GameLogic();

        Board board = BoardStateSerializer.deserialize(session.getBoardStateJson());
        com.dame.engine.Player currentPlayer = com.dame.engine.Player.valueOf(session.getCurrentTurn());
        Position multiJumpPos = BoardStateSerializer.deserializePosition(session.getMultiJumpPositionJson());

        game.restoreState(board, currentPlayer, session.getGameState(), multiJumpPos);

        return game;
    }

    private void handleGameEnd(OnlineGameSession session, GameState result) {
        session.setGamesPlayed(session.getGamesPlayed() + 1);

        if (result == GameState.WHITE_WINS) {
            session.setWhiteWins(session.getWhiteWins() + 1);
        } else if (result == GameState.BLACK_WINS) {
            session.setBlackWins(session.getBlackWins() + 1);
        }

        // Keep session in progress for rematch capability
        // Only set to COMPLETED if player explicitly leaves
    }

    /**
     * Request a rematch from the opponent.
     */
    @Transactional
    public void requestRematch(Long sessionId, Player player) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return;
        }

        OnlineGameSession session = optSession.get();

        // Validate player is part of this game
        if (!session.hasPlayer(player)) {
            return;
        }

        // Only allow rematch request when a round has ended
        GameLogic game = reconstructGame(session);
        if (!game.isGameOver()) {
            return;
        }

        // Check if there's already a pending request
        if (session.hasPendingRematchRequest()) {
            // If opponent requested, this is effectively an accept
            if (!session.getRematchRequestedBy().getId().equals(player.getId())) {
                acceptRematch(sessionId, player);
                return;
            }
            // Already requested by this player
            return;
        }

        // Set rematch request
        session.setRematchRequestedBy(player);
        session.setRematchRequestedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Broadcast to opponent
        GameUpdate update = GameUpdate.builder(GameUpdate.UpdateType.REMATCH_REQUESTED, sessionId)
                .message(player.getUsername() + " wants a rematch!")
                .build();

        broadcaster.broadcast(sessionId, update);
    }

    /**
     * Accept a pending rematch request.
     */
    @Transactional
    public void acceptRematch(Long sessionId, Player player) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return;
        }

        OnlineGameSession session = optSession.get();

        // Validate player is part of this game
        if (!session.hasPlayer(player)) {
            return;
        }

        // Check there's a pending request from the opponent
        if (!session.hasPendingRematchRequest()) {
            return;
        }

        // Can't accept your own request
        if (session.getRematchRequestedBy().getId().equals(player.getId())) {
            return;
        }

        // Clear the rematch request and start new round
        session.clearRematchRequest();
        sessionRepository.save(session);

        // Delegate to existing startNewRound logic
        startNewRound(sessionId);
    }

    /**
     * Decline a pending rematch request.
     */
    @Transactional
    public void declineRematch(Long sessionId, Player player) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return;
        }

        OnlineGameSession session = optSession.get();

        // Validate player is part of this game
        if (!session.hasPlayer(player)) {
            return;
        }

        // Check there's a pending request
        if (!session.hasPendingRematchRequest()) {
            return;
        }

        // Can't decline your own request
        if (session.getRematchRequestedBy().getId().equals(player.getId())) {
            return;
        }

        // Clear the rematch request
        session.clearRematchRequest();
        sessionRepository.save(session);

        // Broadcast decline
        GameUpdate update = GameUpdate.builder(GameUpdate.UpdateType.REMATCH_DECLINED, sessionId)
                .message(player.getUsername() + " declined the rematch")
                .build();

        broadcaster.broadcast(sessionId, update);
    }

    private String generateSessionCode() {
        return "GAME-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
