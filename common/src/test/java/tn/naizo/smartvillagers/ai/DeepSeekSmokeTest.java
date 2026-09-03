package tn.naizo.smartvillagers.ai;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import tn.naizo.smartvillagers.config.ApiCredentials;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.villager.PersonaOverride;
import tn.naizo.smartvillagers.villager.VillagerContext;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live DeepSeek smoke test. Skips unless DEEPSEEK_API_KEY is set.
 */
class DeepSeekSmokeTest {
    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void liveCompletionWhenKeyPresent() throws Exception {
        String key = System.getenv("DEEPSEEK_API_KEY");
        Assumptions.assumeTrue(key != null && !key.isBlank(), "DEEPSEEK_API_KEY not set");

        // Snapshot defaults are enough; ApiCredentials reads env directly.
        ApiCredentials.reload();
        Assumptions.assumeTrue(ApiCredentials.apiKey().isPresent());

        VillagerPersona persona = VillagerPersona.of(
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                null,
                PersonaOverride.EMPTY.withTrait("cheerful"),
                "minecraft:farmer"
        );
        VillagerContext context = new VillagerContext(
                "It is daytime in a plains village.",
                persona,
                List.of(),
                1,
                "minecraft:farmer"
        );

        DeepSeekProvider provider = new DeepSeekProvider();
        AiResponse response = provider.complete(new AiRequest(context, "Hello! How are the crops?"))
                .get(40, TimeUnit.SECONDS);

        assertTrue(response.ok(), () -> "AI failed: " + response.error());
        assertFalse(response.text().isBlank());
        System.out.println("DeepSeek smoke reply: " + response.text());
    }
}
