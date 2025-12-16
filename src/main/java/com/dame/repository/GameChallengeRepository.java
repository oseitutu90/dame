package com.dame.repository;

import com.dame.entity.ChallengeStatus;
import com.dame.entity.GameChallenge;
import com.dame.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameChallengeRepository extends JpaRepository<GameChallenge, Long> {

    /**
     * Find pending challenges received by a player.
     */
    List<GameChallenge> findByChallengedAndStatusOrderByCreatedAtDesc(
            Player challenged, ChallengeStatus status);

    /**
     * Find pending challenges sent by a player.
     */
    List<GameChallenge> findByChallengerAndStatusOrderByCreatedAtDesc(
            Player challenger, ChallengeStatus status);

    /**
     * Find an existing pending challenge between two players.
     */
    @Query("SELECT c FROM GameChallenge c WHERE " +
           "c.challenger = :challenger AND c.challenged = :challenged " +
           "AND c.status = :status")
    Optional<GameChallenge> findPendingChallenge(
            @Param("challenger") Player challenger,
            @Param("challenged") Player challenged,
            @Param("status") ChallengeStatus status);

    /**
     * Check if there's any pending challenge between two players (in either direction).
     */
    @Query("SELECT COUNT(c) > 0 FROM GameChallenge c WHERE " +
           "((c.challenger = :player1 AND c.challenged = :player2) " +
           "OR (c.challenger = :player2 AND c.challenged = :player1)) " +
           "AND c.status = 'PENDING'")
    boolean existsPendingChallengeBetween(
            @Param("player1") Player player1,
            @Param("player2") Player player2);

    /**
     * Expire old pending challenges.
     */
    @Modifying
    @Query("UPDATE GameChallenge c SET c.status = 'EXPIRED' " +
           "WHERE c.status = 'PENDING' AND c.expiresAt < :now")
    int expireChallenges(@Param("now") LocalDateTime now);

    /**
     * Find all pending challenges for a player (both sent and received).
     */
    @Query("SELECT c FROM GameChallenge c WHERE " +
           "(c.challenger = :player OR c.challenged = :player) " +
           "AND c.status = 'PENDING' " +
           "ORDER BY c.createdAt DESC")
    List<GameChallenge> findAllPendingForPlayer(@Param("player") Player player);
}
