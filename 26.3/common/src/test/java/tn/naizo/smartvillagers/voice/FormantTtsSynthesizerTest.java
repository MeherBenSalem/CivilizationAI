package tn.naizo.smartvillagers.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormantTtsSynthesizerTest {
    private final FormantTtsSynthesizer tts = new FormantTtsSynthesizer();

    @Test
    void englishTextProduces48kPcm() {
        short[] pcm = tts.synthesize(new TtsRequest("Hello there, traveler.", "en_us", 1.0, 140));
        assertTrue(pcm.length > TtsRequest.SAMPLE_RATE_HZ / 5, "expected at least 200ms of audio");
        assertTrue(hasEnergy(pcm));
    }

    @Test
    void otherScriptsStillProduceAudio() {
        short[] pcm = tts.synthesize(new TtsRequest("你好", "zh_cn", 1.0, 150));
        assertTrue(pcm.length > 1000);
        assertTrue(hasEnergy(pcm));
    }

    @Test
    void volumeScalesAmplitude() {
        TtsRequest loud = new TtsRequest("Marketplace", "en_us", 1.0, 140);
        TtsRequest quiet = new TtsRequest("Marketplace", "en_us", 0.25, 140);
        assertTrue(peak(tts.synthesize(loud)) > peak(tts.synthesize(quiet)));
    }

    @Test
    void blankOrMutedTextIsSilent() {
        assertEquals(0, tts.synthesize(new TtsRequest("   ", "en_us", 1.0, 140)).length);
        assertEquals(0, tts.synthesize(new TtsRequest("Hello", "en_us", 0.0, 140)).length);
    }

    @Test
    void localePrefixSelectsLatinRulesWithoutSeparateTtsSetting() {
        assertEquals("es", VoiceLanguage.languagePrefix("es_mx"));
        assertTrue(SimpleG2p.transcribe("hola", "es_es").size() >= 4);
        assertTrue(SimpleG2p.transcribe("hello", "en_us").size() >= 4);
    }

    private static boolean hasEnergy(short[] pcm) {
        for (short sample : pcm) {
            if (Math.abs(sample) > 200) {
                return true;
            }
        }
        return false;
    }

    private static int peak(short[] pcm) {
        int peak = 0;
        for (short sample : pcm) {
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak;
    }
}
