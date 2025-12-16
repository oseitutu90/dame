package com.dame.service;

import com.dame.dto.LobbyUpdate;
import com.dame.dto.OnlinePlayerDTO;
import com.dame.entity.OnlineGameStatus;
import com.dame.entity.Player;
import com.dame.repository.MatchmakingEntryRepository;
import com.dame.repository.OnlineGameSessionRepository;
import com.dame.service.broadcast.LobbyBroadcaster;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks which players are currently online in the lobby.
 * Uses in-memory storage since presence is transient.
 */
@Service
public class OnlinePresenceService {

    /**
     * Map of player ID to last activity timestamp.
     */
    private final Map<Long, PlayerPresence> onlinePlayers = new ConcurrentHashMap<>();

    private final LobbyBroadcaster lobbyBroadcaster;
    private final OnlineGameSessionRepository sessionRepository;
    private final MatchmakingEntryRepository matchmakingRepository;

    /**
     * How long before a player is considered offline (no heartbeat).
     */
    private static final long TIMEOUT_SECONDS = 60;

    public OnlinePresenceService(LobbyBroadcaster lobbyBroadcaster,
                                 OnlineGameSessionRepository sessionRepository,
                                 MatchmakingEntryRepository matchmakingRepository) {
        this.lobbyBroadcaster = lobbyBroadcaster;
        this.sessionRepository = sessionRepository;
        this.matchmakingRepository = matchmakingRepository;
    }

    /**
     * Mark a player as online.
     */
    public void playerOnline(Player player) {
        boolean wasOffline = !onlinePlayers.containsKey(player.getId());

        onlinePlayers.put(player.getId(), new PlayerPresence(player, LocalDateTime.now()));

        if (wasOffline) {
            LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.PLAYER_ONLINE)
                    .playerId(player.getId())
                    .playerUsername(player.getUsername())
                    .build();
            lobbyBroadcaster.broadcast(update);
        }
    }

    /**
     * Mark a player as offline.
     */
    public void playerOffline(Player player) {
        if (onlinePlayers.remove(player.getId()) != null) {
            LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.PLAYER_OFFLINE)
                    .playerId(player.getId())
                    .playerUsername(player.getUsername())
                    .build();
            lobbyBroadcaster.broadcast(update);
        }
    }

    /**
     * Update player's last activity (heartbeat).
     */
    public void heartbeat(Player player) {
        PlayerPresence presence = onlinePlayers.get(player.getId());
        if (presence != null) {
            presence.lastActivity = LocalDateTime.now();
        } else {
            playerOnline(player);
        }
    }

    /**
     * Check if a player is online.
     */
    public boolean isOnline(Long playerId) {
        PlayerPresence presence = onlinePlayers.get(playerId);
        if (presence == null) {
            return false;
        }

        // Check if timed out
        if (presence.lastActivity.plusSeconds(TIMEOUT_SECONDS).isBefore(LocalDateTime.now())) {
            onlinePlayers.remove(playerId);
            return false;
        }

        return true;
    }

    /**
     * Get list of all online players with their status.
     */
    public List<OnlinePlayerDTO> getOnlinePlayers() {
        cleanupStalePresence();

        return onlinePlayers.values().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get list of online players excluding a specific player.
     */
    public List<OnlinePlayerDTO> getOnlinePlayersExcept(Long playerId) {
        return getOnlinePlayers().stream()
                .filter(p -> !p.getId().equals(playerId))
                .collect(Collectors.toList());
    }

    /**
     * Get count of online players.
     */
    public int getOnlineCount() {
        cleanupStalePresence();
        return onlinePlayers.size();
    }

    private OnlinePlayerDTO toDTO(PlayerPresence presence) {
        Player player = presence.player;

        // Check if player is in an active game
        boolean inGame = sessionRepository.countByPlayerAndStatus(
                player, OnlineGameStatus.IN_PROGRESS) > 0;

        // Check if player is in matchmaking queue
        boolean inQueue = matchmakingRepository.existsByPlayerAndActiveTrue(player);

        // Get stats
        int wins = 0;
        int losses = 0;
        if (player.getStats() != null) {
            wins = player.getStats().getTotalWins();
            losses = player.getStats().getTotalLosses();
        }

        return new OnlinePlayerDTO(
                player.getId(),
                player.getUsername(),
                inGame,
                inQueue,
                wins,
                losses
        );
    }

    private void cleanupStalePresence() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(TIMEOUT_SECONDS);
        onlinePlayers.entrySet().removeIf(e ->
                e.getValue().lastActivity.isBefore(cutoff));
    }

    /**
     * Internal class to track player presence.
     */
    private static class PlayerPresence {
        final Player player;
        LocalDateTime lastActivity;

        PlayerPresence(Player player, LocalDateTime lastActivity) {
            this.player = player;
            this.lastActivity = lastActivity;
        }
    }
}
