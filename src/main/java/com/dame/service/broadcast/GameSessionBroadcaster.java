package com.dame.service.broadcast;

import com.dame.dto.GameUpdate;
import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Broadcasts game updates to all connected clients viewing a game session.
 * Uses Vaadin Push to deliver real-time updates.
 */
@Component
public class GameSessionBroadcaster {

    /**
     * Map of session ID to set of listeners.
     */
    private final Map<Long, Set<Consumer<GameUpdate>>> listeners = new ConcurrentHashMap<>();

    /**
     * Register a listener for game updates on a specific session.
     *
     * @param sessionId the game session ID
     * @param listener the callback to invoke on updates
     * @return a registration that can be used to unregister
     */
    public Registration register(Long sessionId, Consumer<GameUpdate> listener) {
        listeners.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>()).add(listener);

        return () -> {
            Set<Consumer<GameUpdate>> sessionListeners = listeners.get(sessionId);
            if (sessionListeners != null) {
                sessionListeners.remove(listener);
                if (sessionListeners.isEmpty()) {
                    listeners.remove(sessionId);
                }
            }
        };
    }

    /**
     * Broadcast an update to all listeners of a session.
     *
     * @param sessionId the game session ID
     * @param update the update to broadcast
     */
    public void broadcast(Long sessionId, GameUpdate update) {
        Set<Consumer<GameUpdate>> sessionListeners = listeners.get(sessionId);
        if (sessionListeners != null) {
            for (Consumer<GameUpdate> listener : sessionListeners) {
                try {
                    listener.accept(update);
                } catch (Exception e) {
                    // Log error but continue broadcasting to other listeners
                    System.err.println("Error broadcasting to listener: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get count of listeners for a session (for debugging/monitoring).
     */
    public int getListenerCount(Long sessionId) {
        Set<Consumer<GameUpdate>> sessionListeners = listeners.get(sessionId);
        return sessionListeners != null ? sessionListeners.size() : 0;
    }

    /**
     * Check if a session has any listeners.
     */
    public boolean hasListeners(Long sessionId) {
        Set<Consumer<GameUpdate>> sessionListeners = listeners.get(sessionId);
        return sessionListeners != null && !sessionListeners.isEmpty();
    }
}
