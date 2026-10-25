package tn.naizo.smartvillagers.voice;

public record TtsRequest(String text, String locale, double volume, int pitchHz) {
    public static final int SAMPLE_RATE_HZ = 48_000;

    public TtsRequest {
        text = text == null ? "" : text;
        locale = VoiceLanguage.passthrough(locale);
        if (Double.isNaN(volume) || Double.isInfinite(volume)) {
            volume = 1.0;
        }
        volume = Math.max(0.0, Math.min(1.0, volume));
        if (pitchHz < 80) {
            pitchHz = 80;
        } else if (pitchHz > 280) {
            pitchHz = 280;
        }
    }
}
