package tn.naizo.smartvillagers.village;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

/**
 * What one villager knows about each player beyond the chat history: the request they made, when they will ask
 * again, and what they heard from their neighbours. Stored on the villager as one string so every Minecraft
 * version saves it the same way.
 */
public final class VillagerTies {
    public static final int MAX_HEARD = 4;

    private final Map<UUID, VillageRequest> requests = new LinkedHashMap<>();
    private final Map<UUID, Long> nextRequestAt = new LinkedHashMap<>();
    private final Map<UUID, List<String>> heard = new LinkedHashMap<>();

    public Optional<VillageRequest> request(UUID player) {
        return Optional.ofNullable(requests.get(player));
    }

    public void setRequest(UUID player, VillageRequest request) {
        if (request == null) {
            requests.remove(player);
        } else {
            requests.put(player, request);
        }
    }

    public Map<UUID, VillageRequest> requests() {
        return Map.copyOf(requests);
    }

    /** Game time before which this villager will not offer the player another request. */
    public long nextRequestAt(UUID player) {
        return nextRequestAt.getOrDefault(player, 0L);
    }

    public void setNextRequestAt(UUID player, long gameTime) {
        nextRequestAt.put(player, gameTime);
    }

    public List<String> heard(UUID player) {
        return List.copyOf(heard.getOrDefault(player, List.of()));
    }

    /** Remembers news about a player (newest last, oldest dropped). */
    public void hear(UUID player, String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        List<String> notes = heard.computeIfAbsent(player, ignored -> new ArrayList<>());
        notes.remove(note);
        notes.add(note);
        while (notes.size() > MAX_HEARD) {
            notes.remove(0);
        }
    }

    public boolean isEmpty() {
        return requests.isEmpty() && nextRequestAt.isEmpty() && heard.isEmpty();
    }

    public void clear() {
        requests.clear();
        nextRequestAt.clear();
        heard.clear();
    }

    /** Line-based: {@code R <uuid> <request>}, {@code N <uuid> <time>}, {@code H <uuid> <base64 note>}. */
    public String encode() {
        StringBuilder out = new StringBuilder("t1\n");
        requests.forEach((id, request) -> out.append("R ").append(id).append(' ').append(request.encode()).append('\n'));
        nextRequestAt.forEach((id, time) -> out.append("N ").append(id).append(' ').append(time).append('\n'));
        heard.forEach((id, notes) -> notes.forEach(note -> out.append("H ").append(id).append(' ')
                .append(Base64.getEncoder().encodeToString(note.getBytes(StandardCharsets.UTF_8))).append('\n')));
        return out.toString();
    }

    public static VillagerTies decode(String encoded) {
        VillagerTies ties = new VillagerTies();
        if (encoded == null || !encoded.startsWith("t1\n")) {
            return ties;
        }
        for (String line : encoded.split("\n")) {
            String[] parts = line.split(" ", 3);
            if (parts.length != 3) {
                continue;
            }
            try {
                UUID id = UUID.fromString(parts[1]);
                switch (parts[0]) {
                    case "R" -> VillageRequest.decode(parts[2]).ifPresent(r -> ties.requests.put(id, r));
                    case "N" -> ties.nextRequestAt.put(id, Long.parseLong(parts[2]));
                    case "H" -> ties.hear(id, new String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8));
                    default -> {
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // A damaged line loses only itself.
            }
        }
        return ties;
    }
}
