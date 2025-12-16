package com.dame.service;

import com.dame.dto.LobbyUpdate;
import com.dame.entity.ChallengeStatus;
import com.dame.entity.GameChallenge;
import com.dame.entity.OnlineGameSession;
import com.dame.entity.Player;
import com.dame.repository.GameChallengeRepository;
import com.dame.service.broadcast.LobbyBroadcaster;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles challenge requests between players.
 */
@Service
public class ChallengeService {

    private final GameChallengeRepository challengeRepository;
    private final OnlineGameService gameService;
    private final LobbyBroadcaster lobbyBroadcaster;

    public ChallengeService(GameChallengeRepository challengeRepository,
                           OnlineGameService gameService,
                           LobbyBroadcaster lobbyBroadcaster) {
        this.challengeRepository = challengeRepository;
        this.gameService = gameService;
        this.lobbyBroadcaster = lobbyBroadcaster;
    }

    /**
     * Create a challenge from one player to another.
     */
    @Transactional
    public GameChallenge createChallenge(Player challenger, Player challenged, String message) {
        // Check for existing pending challenge between these players
        if (challengeRepository.existsPendingChallengeBetween(challenger, challenged)) {
            throw new IllegalStateException("A challenge already exists between these players");
        }

        // Don't allow challenging yourself
        if (challenger.getId().equals(challenged.getId())) {
            throw new IllegalArgumentException("Cannot challenge yourself");
        }

        GameChallenge challenge = new GameChallenge(challenger, challenged);
        challenge.setMessage(message);
        challenge = challengeRepository.save(challenge);

        // Broadcast to lobby (specifically the challenged player will pick this up)
        LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.CHALLENGE_RECEIVED)
                .challengeId(challenge.getId())
                .playerId(challenger.getId())
                .playerUsername(challenger.getUsername())
                .message(message != null ? message : challenger.getUsername() + " challenges you!")
                .build();
        lobbyBroadcaster.broadcast(update);

        return challenge;
    }

    /**
     * Accept a challenge.
     */
    @Transactional
    public OnlineGameSession acceptChallenge(Long challengeId, Player player) {
        Optional<GameChallenge> optChallenge = challengeRepository.findById(challengeId);

        if (optChallenge.isEmpty()) {
            throw new IllegalArgumentException("Challenge not found");
        }

        GameChallenge challenge = optChallenge.get();

        // Verify this player is the challenged one
        if (!challenge.getChallenged().getId().equals(player.getId())) {
            throw new IllegalArgumentException("You cannot accept this challenge");
        }

        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new IllegalStateException("Challenge is no longer pending");
        }

        if (challenge.isExpired()) {
            challenge.setStatus(ChallengeStatus.EXPIRED);
            challengeRepository.save(challenge);
            throw new IllegalStateException("Challenge has expired");
        }

        // Create game session (challenger is white, challenged is black)
        OnlineGameSession session = gameService.createSession(
                challenge.getChallenger(),
                challenge.getChallenged()
        );

        // Update challenge
        challenge.setStatus(ChallengeStatus.ACCEPTED);
        challenge.setRespondedAt(LocalDateTime.now());
        challenge.setGameSession(session);
        challengeRepository.save(challenge);

        // Broadcast game started
        LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.GAME_STARTED)
                .sessionId(session.getId())
                .sessionCode(session.getSessionCode())
                .challengeId(challengeId)
                .message(challenge.getChallenger().getUsername() + " vs " +
                        challenge.getChallenged().getUsername())
                .build();
        lobbyBroadcaster.broadcast(update);

        return session;
    }

    /**
     * Decline a challenge.
     */
    @Transactional
    public void declineChallenge(Long challengeId, Player player) {
        Optional<GameChallenge> optChallenge = challengeRepository.findById(challengeId);

        if (optChallenge.isEmpty()) {
            return;
        }

        GameChallenge challenge = optChallenge.get();

        // Verify this player is the challenged one
        if (!challenge.getChallenged().getId().equals(player.getId())) {
            return;
        }

        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            return;
        }

        challenge.setStatus(ChallengeStatus.DECLINED);
        challenge.setRespondedAt(LocalDateTime.now());
        challengeRepository.save(challenge);

        // Notify challenger
        LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.CHALLENGE_CANCELLED)
                .challengeId(challengeId)
                .playerId(player.getId())
                .playerUsername(player.getUsername())
                .message(player.getUsername() + " declined your challenge")
                .build();
        lobbyBroadcaster.broadcast(update);
    }

    /**
     * Cancel a challenge (by the challenger).
     */
    @Transactional
    public void cancelChallenge(Long challengeId, Player player) {
        Optional<GameChallenge> optChallenge = challengeRepository.findById(challengeId);

        if (optChallenge.isEmpty()) {
            return;
        }

        GameChallenge challenge = optChallenge.get();

        // Verify this player is the challenger
        if (!challenge.getChallenger().getId().equals(player.getId())) {
            return;
        }

        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            return;
        }

        challenge.setStatus(ChallengeStatus.CANCELLED);
        challenge.setRespondedAt(LocalDateTime.now());
        challengeRepository.save(challenge);

        // Notify challenged player
        LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.CHALLENGE_CANCELLED)
                .challengeId(challengeId)
                .playerId(player.getId())
                .playerUsername(player.getUsername())
                .message(player.getUsername() + " cancelled their challenge")
                .build();
        lobbyBroadcaster.broadcast(update);
    }

    /**
     * Get pending challenges for a player (received).
     */
    public List<GameChallenge> getPendingChallengesReceived(Player player) {
        return challengeRepository.findByChallengedAndStatusOrderByCreatedAtDesc(
                player, ChallengeStatus.PENDING);
    }

    /**
     * Get pending challenges sent by a player.
     */
    public List<GameChallenge> getPendingChallengesSent(Player player) {
        return challengeRepository.findByChallengerAndStatusOrderByCreatedAtDesc(
                player, ChallengeStatus.PENDING);
    }

    /**
     * Periodic cleanup of expired challenges.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void expireChallenges() {
        int expired = challengeRepository.expireChallenges(LocalDateTime.now());

        if (expired > 0) {
            LobbyUpdate update = LobbyUpdate.builder(LobbyUpdate.UpdateType.CHALLENGE_EXPIRED)
                    .message(expired + " challenge(s) expired")
                    .build();
            lobbyBroadcaster.broadcast(update);
        }
    }
}
