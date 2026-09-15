package tn.naizo.smartvillagers.voice;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Emits one clear startup line about spoken voice vs text-only.
 */
public final class VoiceStartupLog {
    private static final AtomicBoolean LOGGED = new AtomicBoolean();

    private VoiceStartupLog() {
    }

    public static String message(boolean enabled, boolean voiceModPresent) {
        if (!enabled) {
            return "Spoken villager voice is disabled in config. Villagers use text chat.";
        }
        if (!voiceModPresent) {
            return "Spoken villager voice skipped: Simple Voice Chat is not installed. "
                    + "Villagers use text chat. Install Simple Voice Chat to hear replies out loud.";
        }
        return "Spoken villager voice enabled (Simple Voice Chat). "
                + "Language follows the AI reply / player game language; there is no separate TTS locale setting.";
    }

    public static boolean emitOnce(Consumer<String> log, boolean enabled, boolean voiceModPresent) {
        if (!LOGGED.compareAndSet(false, true)) {
            return false;
        }
        log.accept(message(enabled, voiceModPresent));
        return true;
    }

    static void resetForTests() {
        LOGGED.set(false);
    }
}
