package com.dame.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a player in the matchmaking queue.
 */
@Entity
@Table(name = "matchmaking_entries", indexes = {
    @Index(name = "idx_matchmaking_active", columnList = "active"),
    @Index(name = "idx_matchmaking_joined_at", columnList = "joinedAt")
})
public class MatchmakingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The player in the queue
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * When the player joined the queue
     */
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    /**
     * Whether this entry is still active (not matched or removed)
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * The game session created when matched (null while waiting)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_session_id")
    private OnlineGameSession matchedSession;

    /**
     * When the player was matched
     */
    private LocalDateTime matchedAt;

    public MatchmakingEntry() {
    }

    public MatchmakingEntry(Player player) {
        this.player = player;
        this.joinedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OnlineGameSession getMatchedSession() {
        return matchedSession;
    }

    public void setMatchedSession(OnlineGameSession matchedSession) {
        this.matchedSession = matchedSession;
    }

    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(LocalDateTime matchedAt) {
        this.matchedAt = matchedAt;
    }
}
