package tn.naizo.smartvillagers.chat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.session.ConversationSessionManager;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class VillagerSelector {
    private VillagerSelector() {
    }

    public static List<VillagerCandidate> findCandidates(ServerPlayer player, String message, MessageSignals signals,
                                                         ConversationSessionManager sessions) {
        double radius = SmartVillagersConfig.get().hearingRadius();
        AABB box = player.getBoundingBox().inflate(radius);
        List<Villager> villagers = player.serverLevel().getEntitiesOfClass(Villager.class, box, Villager::isAlive);

        List<VillagerCandidate> candidates = new ArrayList<>();
        for (Villager villager : villagers) {
            double distance = player.distanceTo(villager);
            if (distance > radius) {
                continue;
            }
            VillagerPersona persona = VillagerPersona.from(villager);
            double score = score(player, villager, persona, message, signals, distance, sessions);
            candidates.add(new VillagerCandidate(villager, persona, distance, score));
        }

        candidates.sort(Comparator.comparingDouble(VillagerCandidate::score).reversed());
        return candidates;
    }

    public static Optional<VillagerCandidate> best(ServerPlayer player, String message, MessageSignals signals,
                                                   ConversationSessionManager sessions) {
        return findCandidates(player, message, signals, sessions).stream().findFirst();
    }

    public static boolean isLookingAt(ServerPlayer player, Villager villager) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 toVillager = villager.getEyePosition().subtract(player.getEyePosition()).normalize();
        return look.dot(toVillager) > 0.92D;
    }

    private static double score(ServerPlayer player, Villager villager, VillagerPersona persona, String message,
                                MessageSignals signals, double distance, ConversationSessionManager sessions) {
        double score = Math.max(0.0, SmartVillagersConfig.get().hearingRadius() - distance);

        if (isLookingAt(player, villager)) {
            score += 6.0;
        }

        if (signals.nameMention() && persona.displayName() != null) {
            if (message.toLowerCase().contains(persona.displayName().toLowerCase())) {
                score += 8.0;
            }
        }

        UUID sessionVillager = sessions.activeVillager(player.getUUID()).orElse(null);
        if (sessionVillager != null && sessionVillager.equals(villager.getUUID())) {
            score += 5.0;
        }

        return score;
    }
}
