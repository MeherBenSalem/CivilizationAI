package tn.naizo.smartvillagers.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.platform.Services;

import java.nio.file.Path;
import java.util.Optional;

public final class ApiCredentials {
    private static final String ENV_KEY = "DEEPSEEK_API_KEY";
    private static volatile boolean loaded;
    private static volatile String cachedKey;
    private static volatile String cachedSource = "none";

    private ApiCredentials() {
    }

    public static Optional<String> apiKey() {
        ensureLoaded();
        return Optional.ofNullable(cachedKey).filter(key -> !key.isBlank());
    }

    public static String source() {
        ensureLoaded();
        return cachedSource;
    }

    public static void reload() {
        loaded = false;
        cachedKey = null;
        cachedSource = "none";
        ensureLoaded();
    }

    static Resolved resolve(String env, String secretsKey, String configKey) {
        if (notBlank(env)) {
            return new Resolved(env.trim(), "environment");
        }
        if (notBlank(secretsKey)) {
            return new Resolved(secretsKey.trim(), "secrets.toml");
        }
        if (notBlank(configKey)) {
            return new Resolved(configKey.trim(), "config.toml");
        }
        return new Resolved(null, "none");
    }

    record Resolved(String key, String source) {
        boolean isPresent() {
            return key != null && !key.isBlank();
        }
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        String env = System.getenv(ENV_KEY);
        String secretsKey = readTomlValue(configDir().resolve("secrets.toml"), "apiKey");
        String configKey = readTomlValue(configDir().resolve("config.toml"), "ai.apiKey");
        Resolved resolved = resolve(env, secretsKey, configKey);
        cachedKey = resolved.key();
        cachedSource = resolved.source();
        loaded = true;
    }

    private static Path configDir() {
        return Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID);
    }

    private static String readTomlValue(Path path, String key) {
        if (path == null || !path.toFile().exists()) {
            return null;
        }
        try {
            CommentedFileConfig file = CommentedFileConfig.builder(path).build();
            file.load();
            Object value = file.get(key);
            file.close();
            return value instanceof String s ? s : null;
        } catch (Exception e) {
            Constants.LOG.warn("Failed to read {} ({})", path.getFileName(), key);
            return null;
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
