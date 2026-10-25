package tn.naizo.smartvillagers.voice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceStartupLogTest {
    @AfterEach
    void reset() {
        VoiceStartupLog.resetForTests();
    }

    @Test
    void absentModMessageIsTextOnlyAndMentionsSimpleVoiceChat() {
        String message = VoiceStartupLog.message(true, false);
        assertTrue(message.contains("Simple Voice Chat"));
        assertTrue(message.toLowerCase().contains("text"));
        assertTrue(message.contains("not installed"));
    }

    @Test
    void presentModMessageExplainsLanguagePassthrough() {
        String message = VoiceStartupLog.message(true, true);
        assertTrue(message.toLowerCase().contains("enabled"));
        assertTrue(message.contains("Simple Voice Chat"));
        assertTrue(message.toLowerCase().contains("language"));
        assertTrue(message.contains("no separate TTS locale"));
    }

    @Test
    void disabledConfigMessage() {
        assertTrue(VoiceStartupLog.message(false, true).contains("disabled"));
    }

    @Test
    void emitsOnlyOnce() {
        List<String> lines = new ArrayList<>();
        assertTrue(VoiceStartupLog.emitOnce(lines::add, true, false));
        assertFalse(VoiceStartupLog.emitOnce(lines::add, true, true));
        assertEquals(1, lines.size());
        assertEquals(VoiceStartupLog.message(true, false), lines.get(0));
    }

    @Test
    void probeUsesVoiceChatModId() {
        assertEquals("voicechat", VoiceAvailability.SIMPLE_VOICE_CHAT_MOD_ID);
        assertTrue(VoiceAvailability.isModLoaded("voicechat"::equals));
        assertFalse(VoiceAvailability.isModLoaded(id -> false));
        assertFalse(VoiceAvailability.isModLoaded(null));
    }
}
