package com.dame.dto;

import com.dame.entity.ChatMessage;

import java.time.LocalDateTime;

/**
 * DTO for real-time chat message updates.
 */
public class ChatUpdate {

    private final Long messageId;
    private final Long sessionId;
    private final Long senderId;
    private final String senderUsername;
    private final String content;
    private final LocalDateTime createdAt;
    private final boolean systemMessage;

    public ChatUpdate(Long messageId, Long sessionId, Long senderId, String senderUsername,
                     String content, LocalDateTime createdAt, boolean systemMessage) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.createdAt = createdAt;
        this.systemMessage = systemMessage;
    }

    public static ChatUpdate fromMessage(ChatMessage message) {
        return new ChatUpdate(
                message.getId(),
                message.getGameSession().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getContent(),
                message.getCreatedAt(),
                message.isSystemMessage()
        );
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isSystemMessage() {
        return systemMessage;
    }
}
