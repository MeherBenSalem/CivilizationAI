package tn.naizo.smartvillagers.config;

import com.electronwill.nightconfig.core.Config;
import org.junit.jupiter.api.Test;
import tn.naizo.smartvillagers.ActivationMode;
import tn.naizo.smartvillagers.DisplayMode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Night-config stores whole numbers as {@link Integer}. Defaults typed as
 * {@code long}/{@code double} must not ClassCastException (seen in 1.1.1 logs).
 */
class SmartVillagersConfigNumericCoercionTest {
    @Test
    void playerCooldownMsIntegerDoesNotCrash() {
        Config config = Config.inMemory();
        config.set("ai.playerCooldownMs", 3000);
        config.set("ai.maxReplyChars", 180);
        config.set("ai.globalRequestsPerMinute", 30);
        config.set("ai.maxConcurrent", 3);
        config.set("ai.thinkingDelayMinTicks", 20);
        config.set("ai.thinkingDelayMaxTicks", 60);
        config.set("proximity.hearingRadius", 12);
        config.set("proximity.responseRadius", 16);
        config.set("voice.volume", 1);
        config.set("voice.range", 0);

        SmartVillagersConfig.Snapshot snapshot = assertDoesNotThrow(
                () -> SmartVillagersConfig.readSnapshot(config));

        assertEquals(3000L, snapshot.playerCooldownMs());
        assertEquals(180, snapshot.maxReplyChars());
        assertEquals(12.0, snapshot.hearingRadius());
        assertEquals(16.0, snapshot.responseRadius());
        assertEquals(1.0, snapshot.voiceVolume());
        assertEquals(0.0, snapshot.voiceRange());
    }

    @Test
    void realisticSavedTomlShapeLoads() {
        // Mirrors a CurseForge instance config.toml after first-run + voice keys.
        Config config = Config.inMemory();
        config.set("voice.volume", 1.0);
        config.set("voice.range", 0.0);
        config.set("voice.fallbackToText", true);
        config.set("voice.enabled", true);
        config.set("persona.allowPlayersEditPersona", false);
        config.set("proximity.hearingRadius", 12.0);
        config.set("proximity.responseRadius", 16.0);
        config.set("proximity.activationMode", "SMART");
        config.set("proximity.chatPrefix", "!");
        config.set("proximity.enabled", true);
        config.set("proximity.requirePrefix", false);
        config.set("display.mode", "CHAT");
        config.set("display.cancelGlobalChatWhenTalking", false);
        config.set("ai.apiBaseUrl", "https://api.deepseek.com/chat/completions");
        config.set("ai.globalRequestsPerMinute", 30);
        config.set("ai.maxReplyChars", 180);
        config.set("ai.playerCooldownMs", 3000); // Integer, not Long
        config.set("ai.maxConcurrent", 3);
        config.set("ai.thinkingDelayMaxTicks", 60);
        config.set("ai.thinkingDelayMinTicks", 20);
        config.set("ai.model", "deepseek-flash");
        config.set("privacy.requirePlayerOptIn", true);

        SmartVillagersConfig.Snapshot snapshot = assertDoesNotThrow(
                () -> SmartVillagersConfig.readSnapshot(config));

        assertTrue(snapshot.proximityEnabled());
        assertEquals(ActivationMode.SMART, snapshot.activationMode());
        assertEquals(DisplayMode.CHAT, snapshot.displayMode());
        assertEquals(3000L, snapshot.playerCooldownMs());
        assertEquals("deepseek-flash", snapshot.model());
        assertTrue(snapshot.voiceEnabled());
    }
}
