package tn.naizo.smartvillagers.voice;

public record VoiceDelivery(boolean spoken, boolean showText) {
    public static VoiceDelivery textOnly() {
        return new VoiceDelivery(false, true);
    }
}
