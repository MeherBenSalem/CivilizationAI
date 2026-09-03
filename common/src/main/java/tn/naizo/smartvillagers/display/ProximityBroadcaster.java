package tn.naizo.smartvillagers.display;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;

public final class ProximityBroadcaster {
    private ProximityBroadcaster() {
    }

    public static void broadcast(ServerLevel level, ServerPlayer source, Component message) {
        double radius = SmartVillagersConfig.get().responseRadius();
        AABB box = source.getBoundingBox().inflate(radius);
        for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (target.distanceToSqr(source) <= radius * radius) {
                target.sendSystemMessage(message);
            }
        }
    }

    public static void broadcastNear(ServerLevel level, double x, double y, double z, Component message) {
        double radius = SmartVillagersConfig.get().responseRadius();
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (target.distanceToSqr(x, y, z) <= radius * radius) {
                target.sendSystemMessage(message);
            }
        }
    }
}
