package com.dame.repository;

import com.dame.entity.MatchResult;
import com.dame.entity.Player;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    @Query("SELECT m FROM MatchResult m WHERE m.winner = :player OR m.loser = :player ORDER BY m.playedAt DESC")
    List<MatchResult> findByPlayer(@Param("player") Player player, Pageable pageable);

    @Query("SELECT m FROM MatchResult m WHERE m.winner = :player OR m.loser = :player ORDER BY m.playedAt DESC")
    List<MatchResult> findAllByPlayer(@Param("player") Player player);

    List<MatchResult> findByWinnerOrderByPlayedAtDesc(Player winner);

    List<MatchResult> findByLoserOrderByPlayedAtDesc(Player loser);

    @Query("SELECT COUNT(m) FROM MatchResult m WHERE m.winner = :player")
    long countWinsByPlayer(@Param("player") Player player);

    @Query("SELECT COUNT(m) FROM MatchResult m WHERE m.loser = :player")
    long countLossesByPlayer(@Param("player") Player player);
}
