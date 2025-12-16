package com.dame.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a chat message in an online game session.
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_session_id", columnList = "game_session_id"),
    @Index(name = "idx_chat_created_at", columnList = "createdAt")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The game session this message belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private OnlineGameSession gameSession;

    /**
     * The player who sent the message
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private Player sender;

    /**
     * The message content
     */
    @Column(nullable = false, length = 500)
    private String content;

    /**
     * When the message was sent
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Whether this is a system message (e.g., "Player joined")
     */
    @Column(nullable = false)
    private boolean systemMessage = false;

    public ChatMessage() {
    }

    public ChatMessage(OnlineGameSession gameSession, Player sender, String content) {
        this.gameSession = gameSession;
        this.sender = sender;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Creates a system message (no sender required in display).
     */
    public static ChatMessage systemMessage(OnlineGameSession gameSession, Player sender, String content) {
        ChatMessage msg = new ChatMessage(gameSession, sender, content);
        msg.setSystemMessage(true);
        return msg;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OnlineGameSession getGameSession() {
        return gameSession;
    }

    public void setGameSession(OnlineGameSession gameSession) {
        this.gameSession = gameSession;
    }

    public Player getSender() {
        return sender;
    }

    public void setSender(Player sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(boolean systemMessage) {
        this.systemMessage = systemMessage;
    }
}
