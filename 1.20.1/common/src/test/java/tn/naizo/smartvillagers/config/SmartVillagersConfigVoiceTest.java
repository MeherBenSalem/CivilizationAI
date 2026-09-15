package tn.naizo.smartvillagers.config;

import com.electronwill.nightconfig.core.Config;
import org.junit.jupiter.api.Test;
import tn.naizo.smartvillagers.voice.VoiceSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartVillagersConfigVoiceTest {
    @Test
    void missingVoiceKeysKeepTextDefaults() {
        Config config = Config.inMemory();
        assertTrue(SmartVillagersConfig.ensureVoiceKeys(config));
        assertFalse(SmartVillagersConfig.ensureVoiceKeys(config));

        SmartVillagersConfig.Snapshot snapshot = SmartVillagersConfig.readSnapshot(Config.inMemory());
        VoiceSettings settings = VoiceSettings.from(snapshot);
        assertTrue(settings.enabled());
        assertEquals(1.0, settings.volume());
        assertEquals(0.0, settings.range());
        assertTrue(settings.fallbackToText());
        assertEquals(16.0, settings.effectiveRange(snapshot.responseRadius()));
    }

    @Test
    void customVoiceKeysAreHonoredAndClamped() {
        Config config = Config.inMemory();
        config.set("voice.enabled", false);
        config.set("voice.volume", 2.5);
        config.set("voice.range", -4);
        config.set("voice.fallbackToText", false);

        SmartVillagersConfig.Snapshot snapshot = SmartVillagersConfig.readSnapshot(config);
        assertFalse(snapshot.voiceEnabled());
        assertEquals(1.0, snapshot.voiceVolume());
        assertEquals(0.0, snapshot.voiceRange());
        assertFalse(snapshot.voiceFallbackToText());
    }

    @Test
    void integerVolumeIsAccepted() {
        Config config = Config.inMemory();
        config.set("voice.volume", 1);
        config.set("voice.range", 12);
        SmartVillagersConfig.Snapshot snapshot = SmartVillagersConfig.readSnapshot(config);
        assertEquals(1.0, snapshot.voiceVolume());
        assertEquals(12.0, snapshot.voiceRange());
    }
}
