package tn.naizo.smartvillagers.session;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ConversationLockManager {
    private final Map<UUID, UUID> villagerLocks = new ConcurrentHashMap<>();

    public boolean tryLock(UUID villagerId, UUID playerId) {
        UUID existing = villagerLocks.putIfAbsent(villagerId, playerId);
        if (existing == null || existing.equals(playerId)) {
            villagerLocks.put(villagerId, playerId);
            return true;
        }
        return false;
    }

    public void unlock(UUID villagerId, UUID playerId) {
        villagerLocks.computeIfPresent(villagerId, (id, owner) -> owner.equals(playerId) ? null : owner);
    }

    public Optional<UUID> owner(UUID villagerId) {
        return Optional.ofNullable(villagerLocks.get(villagerId));
    }

    public void clear() {
        villagerLocks.clear();
    }
}
