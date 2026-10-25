package tn.naizo.smartvillagers.chat;

import tn.naizo.smartvillagers.platform.Mc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
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
        List<Villager> villagers = Mc.level(player).getEntitiesOfClass(Villager.class, box, Villager::isAlive);

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
        return lookAlignment(player, villager) > 0.80D;
    }

    /** 1 when the crosshair is on the villager's eyes, falling towards 0.8 at the edge of the look cone. */
    static double lookAlignment(ServerPlayer player, Villager villager) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 toVillager = villager.getEyePosition().subtract(player.getEyePosition()).normalize();
        return look.dot(toVillager);
    }

    public static boolean isAddressing(ServerPlayer player, Villager villager, double distance) {
        return isLookingAt(player, villager) || ActivationDetector.isCloseEnough(distance);
    }

    private static double score(ServerPlayer player, Villager villager, VillagerPersona persona, String message,
                                MessageSignals signals, double distance, ConversationSessionManager sessions) {
        double score = Math.max(0.0, SmartVillagersConfig.get().hearingRadius() - distance);

        // Looking at a villager outweighs the previous conversation partner standing a little closer, and the one
        // under the crosshair beats a neighbour at the edge of the view.
        double alignment = lookAlignment(player, villager);
        if (alignment > 0.80D) {
            double aim = (alignment - 0.80D) / 0.20D;
            score += 4.0 + 16.0 * aim * aim;
        }

        if (signals.nameMention() && persona.displayName() != null) {
            if (message.toLowerCase().contains(persona.displayName().toLowerCase())) {
                score += 8.0;
            }
        }

        UUID sessionVillager = sessions.activeVillager(player.getUUID()).orElse(null);
        if (sessionVillager != null && sessionVillager.equals(villager.getUUID())) {
            score += 4.0;
        }

        return score;
    }
}
