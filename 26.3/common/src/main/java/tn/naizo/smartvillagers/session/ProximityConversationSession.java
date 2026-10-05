package tn.naizo.smartvillagers.session;

import net.minecraft.world.entity.npc.villager.Villager;

import java.util.UUID;

public final class ProximityConversationSession {
    private final UUID playerId;
    private final UUID villagerId;
    private long lastActivityMs;

    public ProximityConversationSession(UUID playerId, UUID villagerId) {
        this.playerId = playerId;
        this.villagerId = villagerId;
        this.lastActivityMs = System.currentTimeMillis();
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID villagerId() {
        return villagerId;
    }

    public long lastActivityMs() {
        return lastActivityMs;
    }

    public void touch() {
        lastActivityMs = System.currentTimeMillis();
    }

    public boolean matches(Villager villager) {
        return villager.getUUID().equals(villagerId);
    }
}
