package com.dame.service.broadcast;

import com.dame.dto.LobbyUpdate;
import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Broadcasts lobby updates to all connected clients in the lobby.
 * Updates include: online players, new challenges, matchmaking status, games to spectate.
 */
@Component
public class LobbyBroadcaster {

    /**
     * Set of all lobby listeners.
     */
    private final Set<Consumer<LobbyUpdate>> listeners = new CopyOnWriteArraySet<>();

    /**
     * Register a listener for lobby updates.
     *
     * @param listener the callback to invoke on updates
     * @return a registration that can be used to unregister
     */
    public Registration register(Consumer<LobbyUpdate> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Broadcast an update to all lobby listeners.
     *
     * @param update the update to broadcast
     */
    public void broadcast(LobbyUpdate update) {
        for (Consumer<LobbyUpdate> listener : listeners) {
            try {
                listener.accept(update);
            } catch (Exception e) {
                // Log error but continue broadcasting to other listeners
                System.err.println("Error broadcasting lobby update: " + e.getMessage());
            }
        }
    }

    /**
     * Get count of listeners (for debugging/monitoring).
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
