package tn.naizo.smartvillagers.config;

import com.electronwill.nightconfig.core.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiCredentialsConfigTest {
    @Test
    void envWinsOverSecretsAndConfig() {
        ApiCredentials.Resolved resolved = ApiCredentials.resolve("env-key", "secrets-key", "config-key");
        assertEquals("env-key", resolved.key());
        assertEquals("environment", resolved.source());
    }

    @Test
    void secretsWinOverConfigWhenEnvMissing() {
        ApiCredentials.Resolved resolved = ApiCredentials.resolve("  ", "secrets-key", "config-key");
        assertEquals("secrets-key", resolved.key());
        assertEquals("secrets.toml", resolved.source());
    }

    @Test
    void configTomlApiKeyIsUsedWhenEnvAndSecretsMissing() {
        ApiCredentials.Resolved resolved = ApiCredentials.resolve(null, null, "sk-test-from-config");
        assertEquals("sk-test-from-config", resolved.key());
        assertEquals("config.toml", resolved.source());
        assertTrue(resolved.isPresent());
    }

    @Test
    void blankConfigKeyIsNotConfigured() {
        ApiCredentials.Resolved resolved = ApiCredentials.resolve(null, "", "  ");
        assertFalse(resolved.isPresent());
        assertEquals("none", resolved.source());
    }

    @Test
    void ensureAiKeyWritesEmptyApiKeyOnce() {
        Config config = Config.inMemory();
        assertTrue(SmartVillagersConfig.ensureAiKey(config));
        assertEquals("", config.get("ai.apiKey"));
        assertFalse(SmartVillagersConfig.ensureAiKey(config));
    }
}
