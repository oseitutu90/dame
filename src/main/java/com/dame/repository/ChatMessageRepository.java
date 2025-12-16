package com.dame.repository;

import com.dame.entity.ChatMessage;
import com.dame.entity.OnlineGameSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all messages for a game session, ordered by time.
     */
    List<ChatMessage> findByGameSessionOrderByCreatedAtAsc(OnlineGameSession gameSession);

    /**
     * Find recent messages for a game session.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.gameSession = :session ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("session") OnlineGameSession session, Pageable pageable);

    /**
     * Count messages in a session.
     */
    long countByGameSession(OnlineGameSession gameSession);

    /**
     * Find messages after a certain ID (for incremental loading).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.gameSession = :session AND m.id > :lastId ORDER BY m.createdAt ASC")
    List<ChatMessage> findMessagesAfterId(@Param("session") OnlineGameSession session, @Param("lastId") Long lastId);
}
