package tn.naizo.smartvillagers.session;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ConversationSessionManager {
    private final Map<UUID, ProximityConversationSession> sessions = new ConcurrentHashMap<>();

    public void start(UUID playerId, UUID villagerId) {
        sessions.put(playerId, new ProximityConversationSession(playerId, villagerId));
    }

    public void touch(UUID playerId) {
        ProximityConversationSession session = sessions.get(playerId);
        if (session != null) {
            session.touch();
        }
    }

    public Optional<UUID> activeVillager(UUID playerId) {
        ProximityConversationSession session = sessions.get(playerId);
        return session == null ? Optional.empty() : Optional.of(session.villagerId());
    }

    public Optional<ProximityConversationSession> session(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public void end(UUID playerId) {
        sessions.remove(playerId);
    }

    public void clear() {
        sessions.clear();
    }

    public void pruneInactive(long timeoutMs) {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> now - entry.getValue().lastActivityMs() > timeoutMs);
    }
}
