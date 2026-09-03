package tn.naizo.smartvillagers.villager;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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

    public void write(ValueOutput output) {
        output.putInt("rapport", rapport);
        ValueOutput.ValueOutputList keys = output.childrenList("playerKeys");
        ValueOutput players = output.child("players");
        for (Map.Entry<UUID, List<Exchange>> entry : exchanges.entrySet()) {
            String id = entry.getKey().toString();
            keys.addChild().putString("id", id);
            ValueOutput.ValueOutputList list = players.childrenList(id);
            for (Exchange exchange : entry.getValue()) {
                ValueOutput item = list.addChild();
                item.putString("player", exchange.playerMessage());
                item.putString("villager", exchange.villagerReply());
            }
        }
    }

    public static VillagerMemory read(ValueInput input) {
        VillagerMemory memory = new VillagerMemory();
        memory.rapport = input.getIntOr("rapport", 0);
        ValueInput players = input.childOrEmpty("players");
        for (ValueInput keyInput : input.childrenListOrEmpty("playerKeys")) {
            String key = keyInput.getStringOr("id", "");
            if (key.isBlank()) {
                continue;
            }
            try {
                UUID playerId = UUID.fromString(key);
                List<Exchange> history = new ArrayList<>();
                for (ValueInput item : players.childrenListOrEmpty(key)) {
                    history.add(new Exchange(
                            item.getStringOr("player", ""),
                            item.getStringOr("villager", "")
                    ));
                }
                memory.exchanges.put(playerId, history);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return memory;
    }
}
