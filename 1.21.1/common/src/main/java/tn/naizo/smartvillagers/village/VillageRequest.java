package tn.naizo.smartvillagers.village;

import java.util.Locale;
import java.util.Optional;

/**
 * One request a villager made to one player. Minecraft-free so it can be tested and stored as a plain string.
 *
 * @param target   item id for {@link RequestKind#FETCH}, entity type id for {@link RequestKind#SLAY}
 * @param baseline the player's kill statistic for the target when the request was accepted (SLAY only)
 * @param createdAt game time (ticks) when the request was offered
 */
public record VillageRequest(RequestKind kind, String target, int count, int reward, int baseline,
                             boolean accepted, long createdAt) {
    private static final String VERSION = "r1";

    public VillageRequest {
        target = target == null ? "" : target;
        count = Math.max(1, count);
        reward = Math.max(0, reward);
        baseline = Math.max(0, baseline);
    }

    public VillageRequest accept(int statBaseline) {
        return new VillageRequest(kind, target, count, reward, statBaseline, true, createdAt);
    }

    /** "wheat", "zombie", "melon slice": the target id's path, readable in prompts and fallback lines. */
    public String targetLabel() {
        int colon = target.indexOf(':');
        return (colon >= 0 ? target.substring(colon + 1) : target).replace('_', ' ');
    }

    /** How far the player is, given the items they carry (FETCH) or their current kill statistic (SLAY). */
    public int progress(int carriedOrKilled) {
        int done = kind == RequestKind.SLAY ? carriedOrKilled - baseline : carriedOrKilled;
        return Math.max(0, Math.min(count, done));
    }

    public boolean isMet(int carriedOrKilled) {
        return progress(carriedOrKilled) >= count;
    }

    /** "bring 16 wheat" / "defeat 6 zombies". */
    public String describe() {
        if (kind == RequestKind.SLAY) {
            return "defeat " + count + " " + plural(targetLabel(), count);
        }
        return "bring " + count + " " + targetLabel();
    }

    public String encode() {
        return String.join(";", VERSION, kind.name(), target, Integer.toString(count), Integer.toString(reward),
                Integer.toString(baseline), accepted ? "1" : "0", Long.toString(createdAt));
    }

    public static Optional<VillageRequest> decode(String encoded) {
        if (encoded == null) {
            return Optional.empty();
        }
        String[] parts = encoded.split(";", -1);
        if (parts.length != 8 || !VERSION.equals(parts[0])) {
            return Optional.empty();
        }
        try {
            RequestKind kind = RequestKind.valueOf(parts[1].toUpperCase(Locale.ROOT));
            if (parts[2].isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new VillageRequest(kind, parts[2], Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), "1".equals(parts[6]),
                    Long.parseLong(parts[7])));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static String plural(String noun, int count) {
        if (count == 1 || noun.endsWith("s")) {
            return noun;
        }
        if (noun.endsWith("ch") || noun.endsWith("sh") || noun.endsWith("x")) {
            return noun + "es";
        }
        return noun + "s";
    }
}
