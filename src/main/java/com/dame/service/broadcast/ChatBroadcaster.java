package com.dame.service.broadcast;

import com.dame.dto.ChatUpdate;
import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Broadcasts chat messages to all connected clients in a game session.
 */
@Component
public class ChatBroadcaster {

    /**
     * Map of session ID to set of chat listeners.
     */
    private final Map<Long, Set<Consumer<ChatUpdate>>> listeners = new ConcurrentHashMap<>();

    /**
     * Register a listener for chat updates on a specific session.
     *
     * @param sessionId the game session ID
     * @param listener the callback to invoke on new messages
     * @return a registration that can be used to unregister
     */
    public Registration register(Long sessionId, Consumer<ChatUpdate> listener) {
        listeners.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>()).add(listener);

        return () -> {
            Set<Consumer<ChatUpdate>> sessionListeners = listeners.get(sessionId);
            if (sessionListeners != null) {
                sessionListeners.remove(listener);
                if (sessionListeners.isEmpty()) {
                    listeners.remove(sessionId);
                }
            }
        };
    }

    /**
     * Broadcast a chat update to all listeners of a session.
     *
     * @param sessionId the game session ID
     * @param update the chat update to broadcast
     */
    public void broadcast(Long sessionId, ChatUpdate update) {
        Set<Consumer<ChatUpdate>> sessionListeners = listeners.get(sessionId);
        if (sessionListeners != null) {
            for (Consumer<ChatUpdate> listener : sessionListeners) {
                try {
                    listener.accept(update);
                } catch (Exception e) {
                    System.err.println("Error broadcasting chat update: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get count of listeners for a session.
     */
    public int getListenerCount(Long sessionId) {
        Set<Consumer<ChatUpdate>> sessionListeners = listeners.get(sessionId);
        return sessionListeners != null ? sessionListeners.size() : 0;
    }
}
