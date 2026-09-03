package tn.naizo.smartvillagers.privacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.platform.Services;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrivacyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {
    }.getType();

    private final Map<UUID, Boolean> consent = new ConcurrentHashMap<>();
    private Path consentFile;

    public void load() {
        consentFile = Services.PLATFORM.getConfigDirectory()
                .resolve(Constants.MOD_ID)
                .resolve("consent.json");
        consent.clear();

        if (!Files.exists(consentFile)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(consentFile)) {
            Map<String, Boolean> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) {
                loaded.forEach((key, value) -> {
                    try {
                        consent.put(UUID.fromString(key), Boolean.TRUE.equals(value));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        } catch (IOException e) {
            Constants.LOG.warn("Failed to load consent file", e);
        }
    }

    public void save() {
        if (consentFile == null) {
            load();
        }
        try {
            Files.createDirectories(consentFile.getParent());
            Map<String, Boolean> serializable = new ConcurrentHashMap<>();
            consent.forEach((uuid, value) -> serializable.put(uuid.toString(), value));
            try (Writer writer = Files.newBufferedWriter(consentFile)) {
                GSON.toJson(serializable, MAP_TYPE, writer);
            }
        } catch (IOException e) {
            Constants.LOG.warn("Failed to save consent file", e);
        }
    }

    public boolean hasConsent(ServerPlayer player) {
        if (!SmartVillagersConfig.get().requirePlayerOptIn()) {
            return true;
        }
        return Boolean.TRUE.equals(consent.get(player.getUUID()));
    }

    public boolean isDeclined(ServerPlayer player) {
        return Boolean.FALSE.equals(consent.get(player.getUUID()));
    }

    public void setConsent(ServerPlayer player, boolean accepted) {
        consent.put(player.getUUID(), accepted);
        save();
    }

    public void clear(ServerPlayer player) {
        consent.remove(player.getUUID());
        save();
    }

    public void sendJoinNotice(ServerPlayer player) {
        if (!SmartVillagersConfig.get().requirePlayerOptIn()) {
            return;
        }
        if (consent.containsKey(player.getUUID())) {
            return;
        }

        player.sendSystemMessage(Component.literal(
                "Nearby villagers may respond to chat using Smart Villagers AI. " +
                        "Use /villagerai consent accept or /villagerai consent decline."
        ));
    }
}
