package com.dame.repository;

import com.dame.entity.MatchmakingEntry;
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
public interface MatchmakingEntryRepository extends JpaRepository<MatchmakingEntry, Long> {

    /**
     * Find active entry for a player.
     */
    Optional<MatchmakingEntry> findByPlayerAndActiveTrue(Player player);

    /**
     * Check if player is in queue.
     */
    boolean existsByPlayerAndActiveTrue(Player player);

    /**
     * Find all active entries ordered by join time (FIFO).
     */
    List<MatchmakingEntry> findByActiveTrueOrderByJoinedAtAsc();

    /**
     * Find first active entry that isn't the given player.
     */
    @Query("SELECT e FROM MatchmakingEntry e WHERE e.active = true AND e.player <> :player ORDER BY e.joinedAt ASC")
    List<MatchmakingEntry> findOtherActiveEntries(@Param("player") Player player);

    /**
     * Deactivate old entries (cleanup).
     */
    @Modifying
    @Query("UPDATE MatchmakingEntry e SET e.active = false WHERE e.active = true AND e.joinedAt < :cutoff")
    int deactivateOldEntries(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Count players in queue.
     */
    long countByActiveTrue();
}
