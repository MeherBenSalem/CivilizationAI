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

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        String env = System.getenv(ENV_KEY);
        if (env != null && !env.isBlank()) {
            cachedKey = env.trim();
            cachedSource = "environment";
            loaded = true;
            return;
        }

        Path secretsPath = Services.PLATFORM.getConfigDirectory()
                .resolve(Constants.MOD_ID)
                .resolve("secrets.toml");

        if (secretsPath.toFile().exists()) {
            try {
                CommentedFileConfig secrets = CommentedFileConfig.builder(secretsPath).build();
                secrets.load();
                String fileKey = secrets.get("apiKey");
                secrets.close();
                if (fileKey != null && !fileKey.isBlank()) {
                    cachedKey = fileKey.trim();
                    cachedSource = "secrets.toml";
                    loaded = true;
                    return;
                }
            } catch (Exception e) {
                Constants.LOG.warn("Failed to read secrets.toml (api key not loaded)");
            }
        }

        cachedKey = null;
        cachedSource = "none";
        loaded = true;
    }
}
