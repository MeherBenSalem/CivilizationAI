package tn.naizo.smartvillagers.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import tn.naizo.smartvillagers.platform.Mc;
import tn.naizo.smartvillagers.village.RequestEvent;
import tn.naizo.smartvillagers.village.Standing;
import tn.naizo.smartvillagers.village.VillageRequest;

import java.util.List;
import java.util.Optional;

public final class VillagerContextBuilder {
    private VillagerContextBuilder() {
    }

    public static VillagerContext from(ServerPlayer player, Villager villager, VillagerMemory memory, VillagerPersona persona) {
        return from(player, villager, memory, persona, Standing.STRANGER, 0, RequestEvent.NONE);
    }

    public static VillagerContext from(ServerPlayer player, Villager villager, VillagerMemory memory, VillagerPersona persona,
                                       Standing standing, int favours, RequestEvent event) {
        ServerLevel level = Mc.level(player);
        String worldFacts = buildWorldFacts(level, player, villager);
        return new VillagerContext(
                worldFacts,
                persona,
                memory.recentFor(player.getUUID(), VillagerMemory.MAX_HISTORY),
                memory.rapport(),
                persona.professionKey(),
                playerLanguage(player),
                relationship(player, villager, standing, favours),
                event.promptFact()
        );
    }

    static String relationship(ServerPlayer player, Villager villager, Standing standing, int favours) {
        StringBuilder text = new StringBuilder("Your attitude to this player: ").append(standing.prompt()).append(". ");
        if (favours > 0) {
            text.append("They have done ").append(favours).append(favours == 1 ? " favour" : " favours")
                    .append(" for villagers in this world. ");
        }
        VillagerAiData data = VillagerAiData.get(villager);
        List<String> heard = data.ties().heard(player.getUUID());
        if (!heard.isEmpty()) {
            text.append("News you heard from neighbours: ").append(String.join(" ", heard)).append(' ');
        }
        Optional<VillageRequest> open = data.ties().request(player.getUUID()).filter(VillageRequest::accepted);
        open.ifPresent(request -> text.append("They are working on your request to ").append(request.describe())
                .append(" for ").append(request.reward()).append(" emeralds. "));
        return text.toString().trim();
    }

    private static String buildWorldFacts(ServerLevel level, ServerPlayer player, Villager villager) {
        StringBuilder facts = new StringBuilder();
        facts.append("Dimension ").append(Mc.dimension(level)).append(". ");
        String biome = Mc.biome(level, villager.blockPosition());
        if (!biome.isEmpty()) {
            facts.append("Biome: ").append(biome).append(". ");
        }
        facts.append("Time of day ").append(Mc.timeOfDay(level)).append(" (0 dawn, 6000 noon, 13000 dusk). ");
        if (level.isRaining()) {
            facts.append("It is raining. ");
        }
        if (level.isThundering()) {
            facts.append("A thunderstorm rages. ");
        }
        if (Mc.isDay(level)) {
            facts.append("Daylight. ");
        } else {
            facts.append("Nighttime. ");
        }
        if (level.isRaided(villager.blockPosition())) {
            facts.append("A raid is attacking the village! ");
        }
        int hostiles = level.getEntitiesOfClass(Monster.class, villager.getBoundingBox().inflate(16.0), Monster::isAlive).size();
        if (hostiles > 0) {
            facts.append(hostiles).append(hostiles == 1 ? " hostile mob is" : " hostile mobs are").append(" lurking nearby. ");
        }
        facts.append("You are a level ").append(Mc.villagerLevel(villager)).append(" ").append("(1 novice to 5 master) villager. ");
        facts.append("Player ").append(Mc.playerName(player)).append(" is nearby");
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            facts.append(", holding ").append(Mc.itemId(held.getItem()).replace("minecraft:", "").replace('_', ' '));
        }
        facts.append(String.format(", health %.0f/%.0f. ", player.getHealth(), player.getMaxHealth()));
        facts.append("Villager at block ").append(villager.blockPosition().toShortString()).append('.');
        return facts.toString().trim();
    }

    public static String playerLanguage(ServerPlayer player) {
        return Mc.language(player);
    }
}
