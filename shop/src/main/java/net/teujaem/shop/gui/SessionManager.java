package net.teujaem.shop.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    public PlayerSession get(UUID uuid) {
        return sessions.computeIfAbsent(uuid, k -> new PlayerSession());
    }

    public void clear(UUID uuid) {
        sessions.remove(uuid);
    }
}
