package com.dame.service;

import com.dame.dto.LobbyUpdate;
import com.dame.entity.MatchmakingEntry;
import com.dame.entity.OnlineGameSession;
import com.dame.entity.Player;
import com.dame.repository.MatchmakingEntryRepository;
import com.dame.service.broadcast.LobbyBroadcaster;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles auto-matchmaking queue for players seeking random opponents.
 */
@Service
public class MatchmakingService {

    private final MatchmakingEntryRepository entryRepository;
    private final OnlineGameService gameService;
    private final LobbyBroadcaster lobbyBroadcaster;

    public MatchmakingService(MatchmakingEntryRepository entryRepository,
                              OnlineGameService gameService,
                              LobbyBroadcaster lobbyBroadcaster) {
        this.entryRepository = entryRepository;
        this.gameService = gameService;
        this.lobbyBroadcaster = lobbyBroadcaster;
    }

    /**
     * Add a player to the matchmaking queue.
     * Returns existing session if match is found immediately.
     */
    @Transactional
    public Optional<OnlineGameSession> joinQueue(Player player) {
        // Check if already in queue
        if (entryRepository.existsByPlayerAndActiveTrue(player)) {
            return Optional.empty();
        }

        // Try to find a match immediately
        List<MatchmakingEntry> otherEntries = entryRepository.findOtherActiveEntries(player);

        if (!otherEntries.isEmpty()) {
            // Match found! Create game
            MatchmakingEntry opponent = otherEntries.get(0);
            return Optional.of(createMatch(player, opponent.getPlayer(), opponent));
        }

        // No match found, join queue
        MatchmakingEntry entry = new MatchmakingEntry(player);
        entryRepository.save(entry);

        // Broadcast that player is searching
        LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.MATCHMAKING_STARTED)
                .playerId(player.getId())
                .playerUsername(player.getUsername())
                .build();
        lobbyBroadcaster.broadcast(update);

        return Optional.empty();
    }

    /**
     * Remove a player from the matchmaking queue.
     */
    @Transactional
    public void leaveQueue(Player player) {
        Optional<MatchmakingEntry> entry = entryRepository.findByPlayerAndActiveTrue(player);

        if (entry.isPresent()) {
            entry.get().setActive(false);
            entryRepository.save(entry.get());

            LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.MATCHMAKING_CANCELLED)
                    .playerId(player.getId())
                    .playerUsername(player.getUsername())
                    .build();
            lobbyBroadcaster.broadcast(update);
        }
    }

    /**
     * Check if a player is in the queue.
     */
    public boolean isInQueue(Player player) {
        return entryRepository.existsByPlayerAndActiveTrue(player);
    }

    /**
     * Get current queue size.
     */
    public long getQueueSize() {
        return entryRepository.countByActiveTrue();
    }

    /**
     * Get a player's queue entry if it has been matched.
     */
    @Transactional
    public Optional<OnlineGameSession> checkForMatch(Player player) {
        Optional<MatchmakingEntry> entry = entryRepository.findByPlayerAndActiveTrue(player);

        if (entry.isPresent() && entry.get().getMatchedSession() != null) {
            return Optional.of(entry.get().getMatchedSession());
        }

        return Optional.empty();
    }

    /**
     * Periodic task to match players and cleanup stale entries.
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    @Transactional
    public void processQueue() {
        List<MatchmakingEntry> activeEntries = entryRepository.findByActiveTrueOrderByJoinedAtAsc();

        // Match pairs of players
        while (activeEntries.size() >= 2) {
            MatchmakingEntry player1Entry = activeEntries.remove(0);
            MatchmakingEntry player2Entry = activeEntries.remove(0);

            createMatch(player1Entry.getPlayer(), player2Entry.getPlayer(),
                    player1Entry, player2Entry);
        }

        // Cleanup entries older than 30 minutes
        entryRepository.deactivateOldEntries(LocalDateTime.now().minusMinutes(30));
    }

    private OnlineGameSession createMatch(Player player1, Player player2,
                                         MatchmakingEntry... entries) {
        // Randomly assign colors (or based on who joined first)
        Player white = player1;
        Player black = player2;

        OnlineGameSession session = gameService.createSession(white, black);

        // Update entries
        LocalDateTime now = LocalDateTime.now();
        for (MatchmakingEntry entry : entries) {
            entry.setActive(false);
            entry.setMatchedSession(session);
            entry.setMatchedAt(now);
            entryRepository.save(entry);
        }

        // Broadcast match found
        LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.MATCHMAKING_FOUND)
                .sessionId(session.getId())
                .sessionCode(session.getSessionCode())
                .message(white.getUsername() + " vs " + black.getUsername())
                .build();
        lobbyBroadcaster.broadcast(update);

        return session;
    }
}
