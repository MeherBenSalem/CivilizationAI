package tn.naizo.smartvillagers.voice;

import java.util.function.Predicate;

public final class VoiceAvailability {
    public static final String SIMPLE_VOICE_CHAT_MOD_ID = "voicechat";

    private VoiceAvailability() {
    }

    public static boolean isModLoaded(Predicate<String> mods) {
        return mods != null && mods.test(SIMPLE_VOICE_CHAT_MOD_ID);
    }
}
