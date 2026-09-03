package tn.naizo.smartvillagers.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public final class VillagerContextBuilder {
    private VillagerContextBuilder() {
    }

    public static VillagerContext from(ServerPlayer player, Villager villager, VillagerMemory memory, VillagerPersona persona) {
        ServerLevel level = player.serverLevel();
        String worldFacts = buildWorldFacts(level, player, villager);
        return new VillagerContext(
                worldFacts,
                persona,
                memory.recentFor(player.getUUID(), VillagerMemory.MAX_HISTORY),
                memory.rapport(),
                persona.professionKey()
        );
    }

    private static String buildWorldFacts(ServerLevel level, ServerPlayer player, Villager villager) {
        StringBuilder facts = new StringBuilder();
        facts.append("Dimension ").append(level.dimension().location()).append(". ");
        facts.append("Time of day ").append(level.getDayTime() % 24000L).append(". ");
        if (level.isRaining()) {
            facts.append("It is raining. ");
        }
        if (level.isThundering()) {
            facts.append("A thunderstorm rages. ");
        }
        if (level.isDay()) {
            facts.append("Daylight. ");
        } else {
            facts.append("Nighttime. ");
        }
        facts.append("Player ").append(player.getGameProfile().getName()).append(" is nearby. ");
        facts.append("Villager at block ").append(villager.blockPosition().toShortString()).append('.');
        return facts.toString().trim();
    }
}
