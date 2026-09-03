package tn.naizo.smartvillagers.villager;

import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;
import java.util.UUID;

public record VillagerPersona(
        String displayName,
        String trait,
        String speechStyle,
        String backstory,
        String professionLabel,
        String professionKey
) {
    private static final List<String> NAMES = List.of(
            "Aldric", "Bram", "Celia", "Dara", "Eldon", "Faye", "Garrick", "Hilda",
            "Ivor", "Juna", "Kael", "Liora", "Milo", "Nessa", "Orin", "Petra",
            "Quinn", "Rhea", "Silas", "Tamsin", "Ulric", "Vera", "Wynn", "Yara", "Zed"
    );

    private static final List<String> TRAITS = List.of(
            "curious", "grumpy", "cheerful", "practical", "storyteller", "cautious",
            "generous", "witty", "pious", "stubborn", "dreamy", "hardworking",
            "warm and talkative", "gruff but fair", "superstitious", "proud of their craft",
            "weary and overworked", "blunt and practical", "quietly observant", "eager to gossip"
    );

    private static final List<String> SPEECH_STYLES = List.of(
            "plain and direct",
            "warm and folksy",
            "formal and measured",
            "quick and playful",
            "slow and thoughtful",
            "blunt but kind"
    );

    private static final List<String> BACKSTORIES = List.of(
            "Grew up tending crops outside the village walls.",
            "Once traveled between villages as a journeyman trader.",
            "Lost a cart to pillagers and started anew here.",
            "Keeps careful notes on every trade and rumor.",
            "Raised by librarians who loved old tales.",
            "Still remembers the first iron golem they saw built."
    );

    public static VillagerPersona of(UUID villagerId, String customName, PersonaOverride override, String professionKey) {
        int seed = villagerId.hashCode();
        String resolvedName = override.name()
                .or(() -> optionalCustomName(customName))
                .orElseGet(() -> pick(NAMES, seed));
        String resolvedTrait = override.trait().orElseGet(() -> pick(TRAITS, seed + 1));
        String resolvedStyle = override.speechStyle().orElseGet(() -> pick(SPEECH_STYLES, seed + 2));
        String resolvedBackstory = override.backstory().orElseGet(() -> pick(BACKSTORIES, seed + 3));
        String label = professionLabelFromKey(professionKey);
        return new VillagerPersona(resolvedName, resolvedTrait, resolvedStyle, resolvedBackstory, label, professionKey);
    }

    public static VillagerPersona from(Villager villager) {
        VillagerAiData data = VillagerAiData.get(villager);
        String professionKey = professionKey(villager);
        String customName = villager.hasCustomName() ? villager.getCustomName().getString() : null;
        return of(villager.getUUID(), customName, data.personaOverride(), professionKey);
    }

    public static String professionKey(Villager villager) {
        return villager.getVillagerData().profession().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("minecraft:none");
    }

    public static String professionFlavor(String professionKey) {
        return switch (professionKey) {
            case "minecraft:farmer" -> "You know the fields, seasons, and crops well.";
            case "minecraft:librarian" -> "You value books, lore, and quiet study.";
            case "minecraft:armorer" -> "You craft and appreciate sturdy armor.";
            case "minecraft:weaponsmith" -> "You respect fine blades and honest work.";
            case "minecraft:toolsmith" -> "You understand tools and craftsmanship.";
            case "minecraft:butcher" -> "You know meats, meals, and village feasts.";
            case "minecraft:cleric" -> "You speak with reverence and gentle counsel.";
            case "minecraft:fletcher" -> "You are skilled with bows and careful aim.";
            case "minecraft:fisherman" -> "You know rivers, patience, and the catch of the day.";
            case "minecraft:cartographer" -> "You love maps, distant places, and exploration.";
            case "minecraft:leatherworker" -> "You work leather and practical goods.";
            case "minecraft:shepherd" -> "You tend flocks and open pastures.";
            case "minecraft:mason" -> "You admire stone, structure, and lasting builds.";
            case "minecraft:nitwit" -> "You wander without a trade but still belong to the village.";
            case "minecraft:none" -> "You have not chosen a profession yet.";
            default -> "You serve your village in your own way.";
        };
    }

    private static String professionLabelFromKey(String professionKey) {
        int slash = professionKey.indexOf(':');
        String raw = slash >= 0 ? professionKey.substring(slash + 1) : professionKey;
        return raw.replace('_', ' ');
    }

    private static java.util.Optional<String> optionalCustomName(String customName) {
        if (customName == null || customName.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(customName.trim());
    }

    private static String pick(List<String> pool, int seed) {
        int index = Math.floorMod(seed, pool.size());
        return pool.get(index);
    }
}
