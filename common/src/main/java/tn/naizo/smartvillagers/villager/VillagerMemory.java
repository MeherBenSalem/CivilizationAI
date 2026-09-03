package tn.naizo.smartvillagers.villager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VillagerMemory {
    public static final int MAX_HISTORY = 8;

    private final Map<UUID, List<Exchange>> exchanges = new LinkedHashMap<>();
    private int rapport;

    public record Exchange(String playerMessage, String villagerReply) {
        public static Exchange fromNbt(CompoundTag tag) {
            return new Exchange(tag.getString("player"), tag.getString("villager"));
        }

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putString("player", playerMessage);
            tag.putString("villager", villagerReply);
            return tag;
        }
    }

    public int rapport() {
        return rapport;
    }

    public void adjustRapport(int delta) {
        rapport = Math.max(-5, Math.min(10, rapport + delta));
    }

    public List<Exchange> historyFor(UUID playerId) {
        return Collections.unmodifiableList(exchanges.getOrDefault(playerId, List.of()));
    }

    public List<Exchange> recentFor(UUID playerId, int limit) {
        List<Exchange> history = exchanges.getOrDefault(playerId, List.of());
        if (history.size() <= limit) {
            return List.copyOf(history);
        }
        return List.copyOf(history.subList(history.size() - limit, history.size()));
    }

    public void remember(UUID playerId, String playerMessage, String villagerReply) {
        List<Exchange> history = exchanges.computeIfAbsent(playerId, ignored -> new ArrayList<>());
        history.add(new Exchange(playerMessage, villagerReply));
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        adjustRapport(1);
    }

    public void clearPlayer(UUID playerId) {
        exchanges.remove(playerId);
    }

    public void clearAll() {
        exchanges.clear();
        rapport = 0;
    }

    public static VillagerMemory fromNbt(CompoundTag tag) {
        VillagerMemory memory = new VillagerMemory();
        if (tag == null || tag.isEmpty()) {
            return memory;
        }
        memory.rapport = tag.getInt("rapport");

        if (tag.contains("players", Tag.TAG_COMPOUND)) {
            CompoundTag players = tag.getCompound("players");
            for (String key : players.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    ListTag list = players.getList(key, Tag.TAG_COMPOUND);
                    List<Exchange> history = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        history.add(Exchange.fromNbt(list.getCompound(i)));
                    }
                    memory.exchanges.put(playerId, history);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return memory;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("rapport", rapport);

        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, List<Exchange>> entry : exchanges.entrySet()) {
            ListTag list = new ListTag();
            for (Exchange exchange : entry.getValue()) {
                list.add(exchange.toNbt());
            }
            players.put(entry.getKey().toString(), list);
        }
        tag.put("players", players);
        return tag;
    }
}
