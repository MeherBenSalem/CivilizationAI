package tn.naizo.smartvillagers.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import tn.naizo.smartvillagers.ActivationMode;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.DisplayMode;
import tn.naizo.smartvillagers.platform.Services;

import java.nio.file.Path;

public final class SmartVillagersConfig {
    private static volatile Snapshot snapshot = Snapshot.defaults();
    private static Path configPath;

    private SmartVillagersConfig() {
    }

    public static void load() {
        configPath = Services.PLATFORM.getConfigDirectory()
                .resolve(Constants.MOD_ID)
                .resolve("config.toml");
        refresh();
    }

    public static void refresh() {
        if (configPath == null) {
            load();
            return;
        }

        try {
            configPath.getParent().toFile().mkdirs();
            CommentedFileConfig config = CommentedFileConfig.builder(configPath)
                    .sync()
                    .autosave()
                    .writingMode(com.electronwill.nightconfig.core.io.WritingMode.REPLACE)
                    .build();

            if (!configPath.toFile().exists()) {
                applyDefaults(config);
                config.save();
            } else {
                config.load();
            }

            snapshot = readSnapshot(config);
            config.close();
        } catch (Exception e) {
            Constants.LOG.error("Failed to load Smart Villagers config; using defaults", e);
            snapshot = Snapshot.defaults();
        }
    }

    public static Snapshot get() {
        return snapshot;
    }

    private static void applyDefaults(Config config) {
        Snapshot defaults = Snapshot.defaults();
        config.set("proximity.enabled", defaults.proximityEnabled());
        config.set("proximity.hearingRadius", defaults.hearingRadius());
        config.set("proximity.responseRadius", defaults.responseRadius());
        config.set("proximity.activationMode", defaults.activationMode().name());
        config.set("proximity.chatPrefix", defaults.chatPrefix());
        config.set("proximity.requirePrefix", defaults.requirePrefix());
        config.set("display.mode", defaults.displayMode().name());
        config.set("display.cancelGlobalChatWhenTalking", defaults.cancelGlobalChatWhenTalking());
        config.set("privacy.requirePlayerOptIn", defaults.requirePlayerOptIn());
        config.set("ai.maxReplyChars", defaults.maxReplyChars());
        config.set("ai.playerCooldownMs", defaults.playerCooldownMs());
        config.set("ai.globalRequestsPerMinute", defaults.globalRequestsPerMinute());
        config.set("ai.maxConcurrent", defaults.maxConcurrent());
        config.set("ai.thinkingDelayMinTicks", defaults.thinkingDelayMinTicks());
        config.set("ai.thinkingDelayMaxTicks", defaults.thinkingDelayMaxTicks());
        config.set("persona.allowPlayersEditPersona", defaults.allowPlayersEditPersona());
        config.set("ai.apiBaseUrl", defaults.apiBaseUrl());
        config.set("ai.model", defaults.model());
    }

    private static Snapshot readSnapshot(Config config) {
        return new Snapshot(
                config.getOrElse("proximity.enabled", true),
                config.getOrElse("proximity.hearingRadius", 12.0),
                config.getOrElse("proximity.responseRadius", 16.0),
                parseEnum(config.getOrElse("proximity.activationMode", "SMART"), ActivationMode.SMART),
                config.getOrElse("proximity.chatPrefix", "!"),
                config.getOrElse("proximity.requirePrefix", false),
                parseEnum(config.getOrElse("display.mode", "CHAT"), DisplayMode.CHAT),
                config.getOrElse("display.cancelGlobalChatWhenTalking", false),
                config.getOrElse("privacy.requirePlayerOptIn", true),
                config.getOrElse("ai.maxReplyChars", 180),
                config.getOrElse("ai.playerCooldownMs", 3000L),
                config.getOrElse("ai.globalRequestsPerMinute", 30),
                config.getOrElse("ai.maxConcurrent", 3),
                config.getOrElse("ai.thinkingDelayMinTicks", 20),
                config.getOrElse("ai.thinkingDelayMaxTicks", 60),
                config.getOrElse("persona.allowPlayersEditPersona", false),
                config.getOrElse("ai.apiBaseUrl", "https://api.deepseek.com/chat/completions"),
                config.getOrElse("ai.model", "deepseek-chat")
        );
    }

    private static <E extends Enum<E>> E parseEnum(String value, E fallback) {
        try {
            @SuppressWarnings("unchecked")
            Class<E> type = (Class<E>) fallback.getClass();
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record Snapshot(
            boolean proximityEnabled,
            double hearingRadius,
            double responseRadius,
            ActivationMode activationMode,
            String chatPrefix,
            boolean requirePrefix,
            DisplayMode displayMode,
            boolean cancelGlobalChatWhenTalking,
            boolean requirePlayerOptIn,
            int maxReplyChars,
            long playerCooldownMs,
            int globalRequestsPerMinute,
            int maxConcurrent,
            int thinkingDelayMinTicks,
            int thinkingDelayMaxTicks,
            boolean allowPlayersEditPersona,
            String apiBaseUrl,
            String model
    ) {
        public static Snapshot defaults() {
            return new Snapshot(
                    true,
                    12.0,
                    16.0,
                    ActivationMode.SMART,
                    "!",
                    false,
                    DisplayMode.CHAT,
                    false,
                    true,
                    180,
                    3000L,
                    30,
                    3,
                    20,
                    60,
                    false,
                    "https://api.deepseek.com/chat/completions",
                    "deepseek-chat"
            );
        }
    }
}
