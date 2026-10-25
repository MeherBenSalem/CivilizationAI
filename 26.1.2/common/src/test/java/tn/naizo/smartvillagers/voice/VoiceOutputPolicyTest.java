package tn.naizo.smartvillagers.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceOutputPolicyTest {
    private static final VoiceSettings ON = new VoiceSettings(true, 1.0, 0.0, true);
    private static final VoiceSettings ON_VOICE_ONLY = new VoiceSettings(true, 1.0, 12.0, false);
    private static final VoiceSettings OFF = new VoiceSettings(false, 1.0, 0.0, false);

    @Test
    void softDepAbsentIsTextOnlyEvenIfVoiceEnabled() {
        VoiceDelivery delivery = VoiceOutputPolicy.resolve(ON_VOICE_ONLY, false, false, true);
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
        assertFalse(VoiceOutputPolicy.canAttempt(ON, false, true));
    }

    @Test
    void disabledConfigNeverSpeaks() {
        VoiceDelivery delivery = VoiceOutputPolicy.resolve(OFF, true, true, true);
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
    }

    @Test
    void backendNotReadyFallsBackToText() {
        VoiceDelivery delivery = VoiceOutputPolicy.resolve(ON_VOICE_ONLY, true, false, true);
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
    }

    @Test
    void enabledWithModAndBackendSpeaksAndKeepsText() {
        VoiceDelivery delivery = VoiceOutputPolicy.resolve(ON, true, true, true);
        assertTrue(delivery.spoken());
        assertTrue(delivery.showText());
    }

    @Test
    void fallbackToTextFalseHidesChatWhenVoicePlays() {
        VoiceDelivery delivery = VoiceOutputPolicy.resolve(ON_VOICE_ONLY, true, true, true);
        assertTrue(delivery.spoken());
        assertFalse(delivery.showText());
    }

    @Test
    void playbackFailureAlwaysShowsText() {
        VoiceDelivery delivery = VoiceOutputPolicy.resolve(ON_VOICE_ONLY, true, true, false);
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
    }

    @Test
    void zeroVolumeDoesNotAttemptSpeech() {
        VoiceSettings muted = new VoiceSettings(true, 0.0, 16.0, false);
        assertFalse(VoiceOutputPolicy.canAttempt(muted, true, true));
        VoiceDelivery delivery = VoiceOutputPolicy.resolve(muted, true, true, true);
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
    }

    @Test
    void effectiveRangeUsesResponseRadiusWhenUnset() {
        assertEquals(16.0, ON.effectiveRange(16.0));
        assertEquals(12.0, ON_VOICE_ONLY.effectiveRange(16.0));
    }
}
