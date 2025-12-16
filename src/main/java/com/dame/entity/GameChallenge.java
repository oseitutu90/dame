package com.dame.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a challenge request from one player to another.
 */
@Entity
@Table(name = "game_challenges")
public class GameChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Player who initiated the challenge
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "challenger_id", nullable = false)
    private Player challenger;

    /**
     * Player who received the challenge
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "challenged_id", nullable = false)
    private Player challenged;

    /**
     * Current status of the challenge
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status = ChallengeStatus.PENDING;

    /**
     * Optional message from challenger
     */
    @Column(length = 200)
    private String message;

    /**
     * When the challenge was created
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * When the challenge expires
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * When the challenge was responded to
     */
    private LocalDateTime respondedAt;

    /**
     * The game session created when challenge is accepted
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id")
    private OnlineGameSession gameSession;

    public GameChallenge() {
    }

    public GameChallenge(Player challenger, Player challenged) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusMinutes(5); // 5 minute expiry
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusMinutes(5);
        }
    }

    /**
     * Checks if the challenge has expired.
     */
    public boolean isExpired() {
        return status == ChallengeStatus.PENDING &&
               LocalDateTime.now().isAfter(expiresAt);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Player getChallenger() {
        return challenger;
    }

    public void setChallenger(Player challenger) {
        this.challenger = challenger;
    }

    public Player getChallenged() {
        return challenged;
    }

    public void setChallenged(Player challenged) {
        this.challenged = challenged;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public OnlineGameSession getGameSession() {
        return gameSession;
    }

    public void setGameSession(OnlineGameSession gameSession) {
        this.gameSession = gameSession;
    }
}
