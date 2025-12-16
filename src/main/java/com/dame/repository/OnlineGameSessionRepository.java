package com.dame.repository;

import com.dame.entity.OnlineGameSession;
import com.dame.entity.OnlineGameStatus;
import com.dame.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineGameSessionRepository extends JpaRepository<OnlineGameSession, Long> {

    /**
     * Find session by unique session code.
     */
    Optional<OnlineGameSession> findBySessionCode(String sessionCode);

    /**
     * Find all active sessions for a player (as white or black).
     */
    @Query("SELECT s FROM OnlineGameSession s WHERE " +
           "(s.whitePlayer = :player OR s.blackPlayer = :player) " +
           "AND s.status IN :statuses")
    List<OnlineGameSession> findByPlayerAndStatusIn(
            @Param("player") Player player,
            @Param("statuses") List<OnlineGameStatus> statuses);

    /**
     * Find all sessions a player is involved in.
     */
    @Query("SELECT s FROM OnlineGameSession s WHERE " +
           "s.whitePlayer = :player OR s.blackPlayer = :player " +
           "ORDER BY s.createdAt DESC")
    List<OnlineGameSession> findByPlayer(@Param("player") Player player);

    /**
     * Find sessions available for spectating (in progress with both players).
     */
    @Query("SELECT s FROM OnlineGameSession s WHERE " +
           "s.status = :status " +
           "AND s.whitePlayer IS NOT NULL AND s.blackPlayer IS NOT NULL " +
           "ORDER BY s.lastMoveAt DESC")
    List<OnlineGameSession> findSpectatable(@Param("status") OnlineGameStatus status);

    /**
     * Find sessions waiting for another player to join.
     */
    List<OnlineGameSession> findByStatusOrderByCreatedAtAsc(OnlineGameStatus status);

    /**
     * Count active games for a player.
     */
    @Query("SELECT COUNT(s) FROM OnlineGameSession s WHERE " +
           "(s.whitePlayer = :player OR s.blackPlayer = :player) " +
           "AND s.status = :status")
    long countByPlayerAndStatus(@Param("player") Player player, @Param("status") OnlineGameStatus status);
}
