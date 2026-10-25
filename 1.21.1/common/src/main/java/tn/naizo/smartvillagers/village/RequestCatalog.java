package tn.naizo.smartvillagers.village;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * The requests each profession can make. Item and mob ids exist in every supported Minecraft version (1.20.1 to
 * 26.3). Higher-level villagers ask for more and pay more.
 */
public final class RequestCatalog {
    /** A request a profession can roll: {@code reward} is the emerald pay at villager level 1. */
    public record Template(RequestKind kind, String target, int min, int max, int reward) {
    }

    private static Template fetch(String item, int min, int max, int reward) {
        return new Template(RequestKind.FETCH, "minecraft:" + item, min, max, reward);
    }

    private static Template slay(String mob, int min, int max, int reward) {
        return new Template(RequestKind.SLAY, "minecraft:" + mob, min, max, reward);
    }

    /** Village defence: any working villager can ask for these. */
    public static final List<Template> DEFENCE = List.of(
            slay("zombie", 5, 8, 3),
            slay("skeleton", 4, 7, 3),
            slay("spider", 4, 7, 3),
            slay("creeper", 3, 5, 4),
            slay("pillager", 3, 5, 5)
    );

    public static final Map<String, List<Template>> BY_PROFESSION = Map.ofEntries(
            Map.entry("minecraft:farmer", List.of(fetch("wheat", 12, 24, 2), fetch("carrot", 12, 24, 2),
                    fetch("potato", 12, 24, 2), fetch("beetroot", 10, 20, 2), fetch("pumpkin", 4, 8, 3),
                    fetch("bone_meal", 12, 24, 2))),
            Map.entry("minecraft:librarian", List.of(fetch("paper", 12, 24, 2), fetch("book", 4, 8, 3),
                    fetch("feather", 8, 16, 2), fetch("ink_sac", 4, 8, 3))),
            Map.entry("minecraft:cartographer", List.of(fetch("paper", 12, 24, 2), fetch("glass_pane", 8, 16, 3),
                    fetch("compass", 1, 2, 4))),
            Map.entry("minecraft:cleric", List.of(fetch("rotten_flesh", 16, 32, 2), fetch("gold_ingot", 3, 6, 3),
                    fetch("redstone", 12, 24, 2), fetch("glass_bottle", 4, 8, 2))),
            Map.entry("minecraft:armorer", List.of(fetch("iron_ingot", 6, 12, 3), fetch("coal", 12, 24, 2),
                    fetch("lava_bucket", 1, 1, 4))),
            Map.entry("minecraft:weaponsmith", List.of(fetch("iron_ingot", 6, 12, 3), fetch("coal", 12, 24, 2),
                    fetch("flint", 8, 16, 2))),
            Map.entry("minecraft:toolsmith", List.of(fetch("iron_ingot", 6, 12, 3), fetch("coal", 12, 24, 2),
                    fetch("stick", 24, 48, 2))),
            Map.entry("minecraft:butcher", List.of(fetch("porkchop", 8, 16, 2), fetch("beef", 8, 16, 2),
                    fetch("chicken", 8, 16, 2), fetch("mutton", 8, 16, 2), fetch("sweet_berries", 12, 24, 2))),
            Map.entry("minecraft:fisherman", List.of(fetch("cod", 8, 16, 2), fetch("salmon", 6, 12, 3),
                    fetch("string", 10, 20, 2), fetch("coal", 10, 20, 2))),
            Map.entry("minecraft:fletcher", List.of(fetch("stick", 24, 48, 2), fetch("flint", 10, 20, 2),
                    fetch("string", 10, 20, 2), fetch("feather", 8, 16, 2))),
            Map.entry("minecraft:leatherworker", List.of(fetch("leather", 8, 16, 3), fetch("rabbit_hide", 6, 12, 3),
                    fetch("flint", 10, 20, 2))),
            Map.entry("minecraft:shepherd", List.of(fetch("white_wool", 12, 24, 2), fetch("string", 10, 20, 2),
                    fetch("shears", 1, 1, 2))),
            Map.entry("minecraft:mason", List.of(fetch("clay_ball", 10, 20, 2), fetch("stone", 32, 64, 2),
                    fetch("quartz", 8, 16, 3), fetch("andesite", 16, 32, 2)))
    );

    private RequestCatalog() {
    }

    /** Whether this profession hands out requests (not unemployed villagers or nitwits). */
    public static boolean offersWork(String professionKey) {
        return BY_PROFESSION.containsKey(professionKey);
    }

    /**
     * Rolls a request for a villager of the given profession and level (1 novice to 5 master). About a third of
     * requests are village defence. Deterministic for a given seed.
     */
    public static Optional<VillageRequest> roll(String professionKey, int level, long seed, long gameTime) {
        List<Template> trade = BY_PROFESSION.get(professionKey);
        if (trade == null) {
            return Optional.empty();
        }
        SplittableRandom random = new SplittableRandom(seed);
        List<Template> pool = random.nextInt(3) == 0 ? DEFENCE : trade;
        Template template = pool.get(random.nextInt(pool.size()));
        int lvl = Math.max(1, Math.min(5, level));
        int count = template.min() == template.max() ? template.min()
                : template.min() + random.nextInt(template.max() - template.min() + 1);
        if (template.max() > 1) {
            // Masters ask for up to twice as much, capped at a stack.
            count = Math.min(64, (int) Math.round(count * (1.0 + 0.25 * (lvl - 1))));
        }
        int reward = template.reward() + (lvl - 1);
        return Optional.of(new VillageRequest(template.kind(), template.target(), count, reward, 0, false, gameTime));
    }
}
