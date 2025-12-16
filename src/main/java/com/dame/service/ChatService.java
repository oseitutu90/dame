package com.dame.service;

import com.dame.dto.ChatUpdate;
import com.dame.entity.ChatMessage;
import com.dame.entity.OnlineGameSession;
import com.dame.entity.Player;
import com.dame.repository.ChatMessageRepository;
import com.dame.repository.OnlineGameSessionRepository;
import com.dame.service.broadcast.ChatBroadcaster;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Handles in-game chat functionality.
 */
@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final OnlineGameSessionRepository sessionRepository;
    private final ChatBroadcaster chatBroadcaster;

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int RECENT_MESSAGES_LIMIT = 50;

    public ChatService(ChatMessageRepository messageRepository,
                      OnlineGameSessionRepository sessionRepository,
                      ChatBroadcaster chatBroadcaster) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.chatBroadcaster = chatBroadcaster;
    }

    /**
     * Send a chat message.
     */
    @Transactional
    public ChatMessage sendMessage(Long sessionId, Player sender, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {
            content = content.substring(0, MAX_MESSAGE_LENGTH);
        }

        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            throw new IllegalArgumentException("Game session not found");
        }

        OnlineGameSession session = optSession.get();

        ChatMessage message = new ChatMessage(session, sender, content.trim());
        message = messageRepository.save(message);

        // Broadcast to all viewers of this session
        ChatUpdate update = ChatUpdate.fromMessage(message);
        chatBroadcaster.broadcast(sessionId, update);

        return message;
    }

    /**
     * Send a system message (e.g., "Player joined", "Game started").
     */
    @Transactional
    public ChatMessage sendSystemMessage(Long sessionId, Player sender, String content) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return null;
        }

        OnlineGameSession session = optSession.get();

        ChatMessage message = ChatMessage.systemMessage(session, sender, content);
        message = messageRepository.save(message);

        ChatUpdate update = ChatUpdate.fromMessage(message);
        chatBroadcaster.broadcast(sessionId, update);

        return message;
    }

    /**
     * Get all messages for a session.
     */
    public List<ChatMessage> getMessages(Long sessionId) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return List.of();
        }

        return messageRepository.findByGameSessionOrderByCreatedAtAsc(optSession.get());
    }

    /**
     * Get recent messages for a session.
     */
    public List<ChatMessage> getRecentMessages(Long sessionId, int limit) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return List.of();
        }

        return messageRepository.findRecentMessages(
                optSession.get(),
                PageRequest.of(0, Math.min(limit, RECENT_MESSAGES_LIMIT))
        );
    }

    /**
     * Get messages after a certain ID (for incremental loading).
     */
    public List<ChatMessage> getMessagesAfter(Long sessionId, Long lastMessageId) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return List.of();
        }

        return messageRepository.findMessagesAfterId(optSession.get(), lastMessageId);
    }

    /**
     * Get message count for a session.
     */
    public long getMessageCount(Long sessionId) {
        Optional<OnlineGameSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return 0;
        }

        return messageRepository.countByGameSession(optSession.get());
    }
}
