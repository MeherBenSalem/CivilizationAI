package tn.naizo.smartvillagers.voice;

import tn.naizo.smartvillagers.config.SmartVillagersConfig;

public record VoiceSettings(
        boolean enabled,
        double volume,
        double range,
        boolean fallbackToText
) {
    public static VoiceSettings from(SmartVillagersConfig.Snapshot snapshot) {
        return new VoiceSettings(
                snapshot.voiceEnabled(),
                snapshot.voiceVolume(),
                snapshot.voiceRange(),
                snapshot.voiceFallbackToText()
        );
    }

    public static VoiceSettings defaults() {
        return from(SmartVillagersConfig.Snapshot.defaults());
    }

    public double effectiveRange(double responseRadius) {
        return range > 0 ? range : responseRadius;
    }
}
