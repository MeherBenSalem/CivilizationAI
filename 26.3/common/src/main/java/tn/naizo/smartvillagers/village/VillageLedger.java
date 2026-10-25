package tn.naizo.smartvillagers.village;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tn.naizo.smartvillagers.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-world count of the requests each player has finished, saved as {@code <world>/smartvillagers/ledger.json}.
 * Every villager in the world knows the player's renown from it.
 */
public final class VillageLedger {
    /** Favours that make a player a hero of the village (and earn the Village Hero advancement). */
    public static final int HERO_FAVOURS = 10;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** A request a player has accepted, indexed so it can be listed without loading the villager. */
    public record Entry(UUID villager, String villagerName, String profession, VillageRequest request) {
    }

    private final Map<UUID, Integer> favours = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Entry>> active = new ConcurrentHashMap<>();
    private Path file;

    public void load(Path file) {
        this.file = file;
        favours.clear();
        active.clear();
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject players = root.has("favours") ? root.getAsJsonObject("favours") : new JsonObject();
            for (String key : players.keySet()) {
                try {
                    favours.put(UUID.fromString(key), Math.max(0, players.get(key).getAsInt()));
                } catch (RuntimeException ignored) {
                    // Skip a damaged entry.
                }
            }
            JsonObject open = root.has("active") ? root.getAsJsonObject("active") : new JsonObject();
            for (String key : open.keySet()) {
                try {
                    UUID player = UUID.fromString(key);
                    for (var element : open.getAsJsonArray(key)) {
                        JsonObject o = element.getAsJsonObject();
                        VillageRequest.decode(o.get("request").getAsString()).ifPresent(request -> {
                            Entry entry = new Entry(UUID.fromString(o.get("villager").getAsString()),
                                    o.get("name").getAsString(), o.get("profession").getAsString(), request);
                            active.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>()).put(entry.villager(), entry);
                        });
                    }
                } catch (RuntimeException ignored) {
                    // Skip a damaged entry.
                }
            }
        } catch (IOException | RuntimeException e) {
            Constants.LOG.warn("Could not read the village ledger {}; starting a new one", file.getFileName());
        }
    }

    public void save() {
        if (file == null) {
            return;
        }
        JsonObject players = new JsonObject();
        favours.forEach((id, count) -> players.addProperty(id.toString(), count));
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("favours", players);
        JsonObject open = new JsonObject();
        active.forEach((player, entries) -> {
            com.google.gson.JsonArray list = new com.google.gson.JsonArray();
            entries.values().forEach(entry -> {
                JsonObject o = new JsonObject();
                o.addProperty("villager", entry.villager().toString());
                o.addProperty("name", entry.villagerName());
                o.addProperty("profession", entry.profession());
                o.addProperty("request", entry.request().encode());
                list.add(o);
            });
            if (!list.isEmpty()) {
                open.add(player.toString(), list);
            }
        });
        root.add("active", open);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Constants.LOG.warn("Could not save the village ledger", e);
        }
    }

    public int favours(UUID player) {
        return favours.getOrDefault(player, 0);
    }

    /** Records a finished request and returns the player's new total. */
    public int recordFavour(UUID player) {
        int total = favours.merge(player, 1, Integer::sum);
        save();
        return total;
    }

    public java.util.List<Entry> active(UUID player) {
        return java.util.List.copyOf(active.getOrDefault(player, Map.of()).values());
    }

    public void track(UUID player, Entry entry) {
        active.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>()).put(entry.villager(), entry);
        save();
    }

    public void untrack(UUID player, UUID villager) {
        Map<UUID, Entry> entries = active.get(player);
        if (entries != null && entries.remove(villager) != null) {
            save();
        }
    }

    public void reset(UUID player) {
        favours.remove(player);
        active.remove(player);
        save();
    }
}
