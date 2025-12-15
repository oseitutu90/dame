package com.dame.repository;

import com.dame.entity.Player;
import com.dame.entity.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {

    Optional<PlayerStats> findByPlayer(Player player);

    Optional<PlayerStats> findByPlayerId(Long playerId);

    @Query("SELECT ps FROM PlayerStats ps ORDER BY ps.totalWins DESC")
    List<PlayerStats> findTopByWins(int limit);

    @Query("SELECT ps FROM PlayerStats ps WHERE ps.matchesPlayed > 0 ORDER BY ps.totalWins DESC")
    List<PlayerStats> findAllOrderByWinsDesc();
}
