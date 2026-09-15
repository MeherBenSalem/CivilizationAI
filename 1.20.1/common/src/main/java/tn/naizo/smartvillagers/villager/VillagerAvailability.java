package tn.naizo.smartvillagers.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class VillagerAvailability {
    private VillagerAvailability() {
    }

    public static Optional<String> busyReply(Villager villager) {
        if (villager.isSleeping()) {
            return Optional.of("Zzz... not now, I'm sleeping.");
        }
        if (villager.isTrading()) {
            return Optional.of("I'm busy trading right now.");
        }
        Level level = villager.level();
        if (level instanceof ServerLevel serverLevel && serverLevel.isRaided(villager.blockPosition())) {
            return Optional.of("Raiders! We must defend the village!");
        }
        return Optional.empty();
    }
}
