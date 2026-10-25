package tn.naizo.smartvillagers.config;

import com.electronwill.nightconfig.core.Config;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProviderConfigTest {
    @Test
    void preservesLegacyFullGatewayRoute() {
        Config config = Config.inMemory();
        config.set("ai.apiBaseUrl", "https://gateway.example/proxy/chat");
        SmartVillagersConfig.ensureProviderKeys(config);
        assertEquals("FULL", config.get("ai.endpointMode"));
    }

    @Test
    void malformedProviderTypeDoesNotInferWorkingProvider() {
        Config config = Config.inMemory();
        config.set("ai.provider", 42);
        assertEquals(AiProviderType.INVALID, SmartVillagersConfig.readSnapshot(config).provider());
        config.set("ai.provider", "");
        assertEquals(AiProviderType.INVALID, SmartVillagersConfig.readSnapshot(config).provider());
    }

    @Test
    void migratesCustomLegacyUrlWithoutOverwritingModelOrKey() {
        Config config = Config.inMemory();
        config.set("ai.apiBaseUrl", "https://example.com/gateway/v1");
        config.set("ai.model", "my-model");
        config.set("ai.apiKey", "my-key");
        assertTrue(SmartVillagersConfig.ensureProviderKeys(config));
        assertFalse(SmartVillagersConfig.ensureProviderKeys(config));
        var snapshot = SmartVillagersConfig.readSnapshot(config);
        assertEquals(AiProviderType.CUSTOM, snapshot.provider());
        assertEquals("https://example.com/gateway/v1", snapshot.apiBaseUrl());
        assertEquals("my-model", snapshot.model());
        assertEquals("my-key", config.get("ai.apiKey"));
    }

    @Test
    void subscriptionPresetCannotFallBackToOldDeepSeekOrPaidUrl() {
        var snapshot = ProviderTestConfig.snapshot("NANOGPT_SUBSCRIPTION", "https://api.deepseek.com", "AUTO");
        assertEquals("https://nano-gpt.com/api/subscription/v1/chat/completions", snapshot.apiBaseUrl());
        assertNotEquals(ProviderTestConfig.snapshot("NANOGPT", "", "AUTO").apiBaseUrl(), snapshot.apiBaseUrl());
    }

    @Test
    void keyEnvironmentIsSpecificToSelectedProvider() {
        Map<String, String> env = Map.of("DEEPSEEK_API_KEY", "wrong-provider", "NANOGPT_API_KEY", "nano-key");
        assertEquals("nano-key", ApiCredentials.environmentKey(AiProviderType.NANOGPT_SUBSCRIPTION, env::get));
        assertNull(ApiCredentials.environmentKey(AiProviderType.CUSTOM, env::get));
        assertEquals("wrong-provider", ApiCredentials.environmentKey(AiProviderType.DEEPSEEK, env::get));
        assertEquals("general-key", ApiCredentials.environmentKey(AiProviderType.NANOGPT,
                Map.of("SMARTVILLAGERS_API_KEY", "general-key", "NANOGPT_API_KEY", "nano-key")::get));
    }

    @Test
    void unknownProviderFailsClosed() {
        assertEquals(AiProviderType.INVALID, ProviderTestConfig.snapshot("NANOGPT_SUBSCRIPTON", "", "AUTO").provider());
        assertEquals("", ProviderTestConfig.snapshot("NANOGPT_SUBSCRIPTON", "", "AUTO").apiBaseUrl());
    }
}
