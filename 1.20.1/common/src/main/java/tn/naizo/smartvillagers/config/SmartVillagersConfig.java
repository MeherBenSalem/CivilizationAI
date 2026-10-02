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
                boolean migrated = ensureVoiceKeys(config) | ensureAiKey(config) | ensureProviderKeys(config);
                if (migrated) {
                    config.save();
                }
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
        config.set("ai.apiKey", "");
        config.set("ai.provider", defaults.provider().name());
        ensureProviderKeys(config);
        applyAiKeyComment(config);
        applyVoiceDefaults(config);
    }

    private static void applyVoiceDefaults(Config config) {
        Snapshot defaults = Snapshot.defaults();
        config.set("voice.enabled", defaults.voiceEnabled());
        config.set("voice.volume", defaults.voiceVolume());
        config.set("voice.range", defaults.voiceRange());
        config.set("voice.fallbackToText", defaults.voiceFallbackToText());
        if (config instanceof com.electronwill.nightconfig.core.CommentedConfig commented) {
            commented.setComment("voice", """
                    Optional spoken replies. No extra TTS language setting: speech uses the same \
                    reply text/language as the AI prompt (player chat + game locale). Requires Simple Voice Chat.""");
            commented.setComment("voice.enabled", "Attempt spoken playback when Simple Voice Chat is installed. Harmless no-op without it.");
            commented.setComment("voice.volume", "Linear gain for synthesized speech, 0.0-1.0. 0 mutes voice and keeps text.");
            commented.setComment("voice.range", "Hearing distance in blocks. 0 uses proximity.responseRadius.");
            commented.setComment("voice.fallbackToText", "If true, still show chat/action-bar text when voice plays. If voice cannot play, text is always shown.");
        }
    }

    /**
     * Writes the [voice] section into an existing config that predates it.
     * @return true if the file should be saved
     */
    static boolean ensureVoiceKeys(Config config) {
        if (config.contains("voice.enabled")) {
            return false;
        }
        applyVoiceDefaults(config);
        return true;
    }

    /**
     * Writes {@code ai.apiKey} into an existing config that predates it.
     * @return true if the file should be saved
     */
    static boolean ensureAiKey(Config config) {
        if (config.contains("ai.apiKey")) {
            return false;
        }
        config.set("ai.apiKey", "");
        applyAiKeyComment(config);
        return true;
    }

    private static void applyAiKeyComment(Config config) {
        if (config instanceof com.electronwill.nightconfig.core.CommentedConfig commented) {
            commented.setComment("ai.apiKey",
                    "Selected provider's API key. SMARTVILLAGERS_API_KEY, provider-specific env and secrets.toml override this. Never share your key.");
        }
    }

    static boolean ensureProviderKeys(Config config) {
        boolean changed = false;
        boolean legacy = !config.contains("ai.provider");
        if (!config.contains("ai.endpointMode")) {
            String mode = "AUTO";
            if (legacy) {
                try {
                    String path = java.net.URI.create(string(config, "ai.apiBaseUrl", "")).getPath();
                    if (path != null && !path.isEmpty() && !path.equals("/") && !path.equals("/v1")) mode = "FULL";
                } catch (RuntimeException ignored) {
                    // Invalid URLs are rejected by the transport.
                }
            }
            config.set("ai.endpointMode", mode);
            changed = true;
        }
        if (!config.contains("ai.provider")) {
            config.set("ai.provider", AiProviderType.parse("", string(config, "ai.apiBaseUrl",
                    "https://api.deepseek.com/chat/completions")).name());
            changed = true;
        }
        if (!config.contains("ai.maxTokens")) {
            config.set("ai.maxTokens", 2048);
            changed = true;
        }
        if (!config.contains("ai.tokenLimitParameter")) {
            config.set("ai.tokenLimitParameter", "AUTO");
            changed = true;
        }
        if (config instanceof com.electronwill.nightconfig.core.CommentedConfig commented) {
            commented.setComment("ai.provider", "DEEPSEEK, NANOGPT, NANOGPT_SUBSCRIPTION, OPENAI, OPENROUTER, OLLAMA, LMSTUDIO or CUSTOM. Presets select their own endpoint; apiBaseUrl is used only for CUSTOM.");
            commented.setComment("ai.apiBaseUrl", "CUSTOM only: OpenAI-compatible base URL or full /chat/completions endpoint. Never put keys in the URL.");
            commented.setComment("ai.endpointMode", "CUSTOM: AUTO appends /chat/completions unless already present. FULL preserves a complete URL, including legacy custom routes.");
            commented.setComment("ai.model", "Exact model ID from the selected provider's model list. NanoGPT subscription requires an included model.");
            commented.setComment("ai.maxTokens", "Completion token budget (including reasoning); replies are separately clipped to maxReplyChars.");
            commented.setComment("ai.tokenLimitParameter", "AUTO, max_tokens, max_completion_tokens or NONE, according to your model/server API.");
            applyAiKeyComment(config);
        }
        return changed;
    }

    static Snapshot readSnapshot(Config config) {
        String customUrl = config.contains("ai.apiBaseUrl")
                ? string(config, "ai.apiBaseUrl", "") : "https://api.deepseek.com/chat/completions";
        Object providerValue = config.get("ai.provider");
        AiProviderType provider = !config.contains("ai.provider") ? AiProviderType.parse("", customUrl)
                : providerValue instanceof String value && !value.isBlank()
                ? AiProviderType.parse(value, customUrl) : AiProviderType.INVALID;
        return new Snapshot(
                bool(config, "proximity.enabled", true),
                number(config, "proximity.hearingRadius", 12.0),
                number(config, "proximity.responseRadius", 16.0),
                parseEnum(string(config, "proximity.activationMode", "SMART"), ActivationMode.SMART),
                string(config, "proximity.chatPrefix", "!"),
                bool(config, "proximity.requirePrefix", false),
                parseEnum(string(config, "display.mode", "CHAT"), DisplayMode.CHAT),
                bool(config, "display.cancelGlobalChatWhenTalking", false),
                bool(config, "privacy.requirePlayerOptIn", true),
                intNumber(config, "ai.maxReplyChars", 180),
                longNumber(config, "ai.playerCooldownMs", 3000L),
                intNumber(config, "ai.globalRequestsPerMinute", 30),
                intNumber(config, "ai.maxConcurrent", 3),
                intNumber(config, "ai.thinkingDelayMinTicks", 20),
                intNumber(config, "ai.thinkingDelayMaxTicks", 60),
                bool(config, "persona.allowPlayersEditPersona", false),
                provider.endpoint(customUrl),
                string(config, "ai.model", "deepseek-flash"),
                provider,
                Math.max(1, Math.min(32768, intNumber(config, "ai.maxTokens", 2048))),
                string(config, "ai.tokenLimitParameter", "AUTO"),
                string(config, "ai.endpointMode", "AUTO"),
                bool(config, "voice.enabled", true),
                clampVolume(number(config, "voice.volume", 1.0)),
                Math.max(0.0, number(config, "voice.range", 0.0)),
                bool(config, "voice.fallbackToText", true)
        );
    }

    private static boolean bool(Config config, String path, boolean fallback) {
        Object value = config.get(path);
        if (value instanceof Boolean b) {
            return b;
        }
        return fallback;
    }

    private static String string(Config config, String path, String fallback) {
        Object value = config.get(path);
        if (value instanceof String s) {
            return s;
        }
        return fallback;
    }

    private static double number(Config config, String path, double fallback) {
        Object value = config.get(path);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    private static int intNumber(Config config, String path, int fallback) {
        Object value = config.get(path);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static long longNumber(Config config, String path, long fallback) {
        Object value = config.get(path);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return fallback;
    }

    private static double clampVolume(double volume) {
        if (Double.isNaN(volume) || Double.isInfinite(volume)) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, volume));
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
            String model,
            AiProviderType provider,
            int maxTokens,
            String tokenLimitParameter,
            String endpointMode,
            boolean voiceEnabled,
            double voiceVolume,
            double voiceRange,
            boolean voiceFallbackToText
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
                    "deepseek-flash",
                    AiProviderType.DEEPSEEK,
                    2048,
                    "AUTO",
                    "AUTO",
                    true,
                    1.0,
                    0.0,
                    true
            );
        }
    }
}
