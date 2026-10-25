package tn.naizo.smartvillagers.config;

import com.electronwill.nightconfig.core.Config;

/** Real config reader for HTTP contract tests; no mutable production state. */
public final class ProviderTestConfig {
    public static SmartVillagersConfig.Snapshot snapshot(String provider, String endpoint, String tokenParameter) {
        Config config = Config.inMemory();
        config.set("ai.provider", provider);
        config.set("ai.apiBaseUrl", endpoint);
        config.set("ai.model", "test-model");
        config.set("ai.tokenLimitParameter", tokenParameter);
        return SmartVillagersConfig.readSnapshot(config);
    }
}
