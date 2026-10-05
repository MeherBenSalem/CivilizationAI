package tn.naizo.smartvillagers.voice;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceCoordinatorTest {
    @Test
    void doesNotSynthesizeWhenVoiceModMissing() {
        RecordingTts tts = new RecordingTts();
        RecordingBackend backend = new RecordingBackend();
        VoiceCoordinator coordinator = new VoiceCoordinator(
                tts,
                () -> backend,
                () -> new VoiceSettings(true, 1.0, 0.0, true),
                () -> false
        );

        VoiceDelivery delivery = coordinator.speak(UUID.randomUUID(), new Object(), "Hello", "en_us", 140, 16);
        assertEquals(0, tts.calls.get());
        assertEquals(0, backend.plays.get());
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
    }

    @Test
    void doesNotSynthesizeWhenDisabled() {
        RecordingTts tts = new RecordingTts();
        RecordingBackend backend = new RecordingBackend();
        VoiceCoordinator coordinator = coordinator(tts, backend, new VoiceSettings(false, 1.0, 8.0, false), true);

        VoiceDelivery delivery = coordinator.speak(UUID.randomUUID(), new Object(), "Hello", "en_us", 140, 16);
        assertEquals(0, tts.calls.get());
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
    }

    @Test
    void playsThroughBackendAndPassesLanguage() {
        RecordingTts tts = new RecordingTts();
        RecordingBackend backend = new RecordingBackend();
        VoiceCoordinator coordinator = coordinator(tts, backend, new VoiceSettings(true, 0.5, 0.0, true), true);
        UUID id = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Object entity = new Object();

        VoiceDelivery delivery = coordinator.speak(id, entity, "Hola", "es_mx", 150, 16);

        assertTrue(delivery.spoken());
        assertTrue(delivery.showText());
        assertEquals(1, tts.calls.get());
        assertEquals("Hola", tts.last.get().text());
        assertEquals("es_mx", tts.last.get().locale());
        assertEquals(0.5, tts.last.get().volume());
        assertEquals(id, backend.last.get().entityId());
        assertEquals(entity, backend.last.get().entity());
        assertEquals(16.0f, backend.last.get().range());
    }

    @Test
    void usesConfiguredRangeWhenSet() {
        RecordingTts tts = new RecordingTts();
        RecordingBackend backend = new RecordingBackend();
        VoiceCoordinator coordinator = coordinator(tts, backend, new VoiceSettings(true, 1.0, 9.5, false), true);

        VoiceDelivery delivery = coordinator.speak(UUID.randomUUID(), new Object(), "Hi", "en_us", 120, 16);
        assertTrue(delivery.spoken());
        assertFalse(delivery.showText());
        assertEquals(9.5f, backend.last.get().range());
    }

    @Test
    void backendFailureFallsBackToText() {
        RecordingTts tts = new RecordingTts();
        RecordingBackend backend = new RecordingBackend();
        backend.playResult = false;
        VoiceCoordinator coordinator = coordinator(tts, backend, new VoiceSettings(true, 1.0, 12.0, false), true);

        VoiceDelivery delivery = coordinator.speak(UUID.randomUUID(), new Object(), "Hi", "en_us", 120, 16);
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
    }

    @Test
    void unreadyBackendIsTextOnly() {
        RecordingTts tts = new RecordingTts();
        RecordingBackend backend = new RecordingBackend();
        backend.ready = false;
        VoiceCoordinator coordinator = coordinator(tts, backend, new VoiceSettings(true, 1.0, 12.0, false), true);

        VoiceDelivery delivery = coordinator.speak(UUID.randomUUID(), new Object(), "Hi", "en_us", 120, 16);
        assertEquals(0, tts.calls.get());
        assertFalse(delivery.spoken());
        assertTrue(delivery.showText());
    }

    private static VoiceCoordinator coordinator(
            RecordingTts tts,
            RecordingBackend backend,
            VoiceSettings settings,
            boolean modPresent
    ) {
        return new VoiceCoordinator(tts, () -> backend, () -> settings, () -> modPresent);
    }

    private static final class RecordingTts implements TtsSynthesizer {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<TtsRequest> last = new AtomicReference<>();
        private final short[] pcm = new short[]{1, 2, 3, 4};

        @Override
        public short[] synthesize(TtsRequest request) {
            calls.incrementAndGet();
            last.set(request);
            return pcm;
        }
    }

    private static final class RecordingBackend implements VoiceBackend {
        private final AtomicInteger plays = new AtomicInteger();
        private final AtomicReference<VoicePlaybackRequest> last = new AtomicReference<>();
        private boolean ready = true;
        private boolean playResult = true;

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public boolean play(VoicePlaybackRequest request) {
            plays.incrementAndGet();
            last.set(request);
            return playResult;
        }
    }
}
