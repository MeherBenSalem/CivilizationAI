package tn.naizo.smartvillagers.voice;

/**
 * Builds a TTS request from a villager reply using the same locale string
 * already used for the AI prompt (player game language).
 */
public final class VillagerSpeech {
    private VillagerSpeech() {
    }

    public static TtsRequest fromReply(String replyText, String playerLanguage, double volume, int pitchHz) {
        return new TtsRequest(replyText, VoiceLanguage.passthrough(playerLanguage), volume, pitchHz);
    }
}
