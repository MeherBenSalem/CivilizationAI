package tn.naizo.smartvillagers.voice;

/**
 * Pure policy for optional spoken output. Safe to unit-test without a game client.
 */
public final class VoiceOutputPolicy {
    private VoiceOutputPolicy() {
    }

    public static boolean canAttempt(VoiceSettings settings, boolean voiceModPresent, boolean backendReady) {
        return settings.enabled() && settings.volume() > 0 && voiceModPresent && backendReady;
    }

    public static VoiceDelivery resolve(
            VoiceSettings settings,
            boolean voiceModPresent,
            boolean backendReady,
            boolean playbackSucceeded
    ) {
        boolean spoken = canAttempt(settings, voiceModPresent, backendReady) && playbackSucceeded;
        boolean showText = settings.fallbackToText() || !spoken;
        return new VoiceDelivery(spoken, showText);
    }
}
