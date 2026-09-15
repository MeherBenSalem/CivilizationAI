package tn.naizo.smartvillagers.voice;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Coordinates optional TTS + voice-mod playback. No Minecraft types — unit-testable.
 */
public final class VoiceCoordinator {
    private final TtsSynthesizer tts;
    private final Supplier<VoiceBackend> backend;
    private final Supplier<VoiceSettings> settings;
    private final BooleanSupplier voiceModPresent;

    public VoiceCoordinator(
            TtsSynthesizer tts,
            Supplier<VoiceBackend> backend,
            Supplier<VoiceSettings> settings,
            BooleanSupplier voiceModPresent
    ) {
        this.tts = tts;
        this.backend = backend;
        this.settings = settings;
        this.voiceModPresent = voiceModPresent;
    }

    public VoiceDelivery speak(
            UUID entityId,
            Object entity,
            String text,
            String playerLanguage,
            int pitchHz,
            double responseRadius
    ) {
        VoiceSettings current = settings.get();
        VoiceBackend voiceBackend = backend.get();
        boolean present = voiceModPresent.getAsBoolean();
        boolean ready = voiceBackend != null && voiceBackend.isReady();
        if (!VoiceOutputPolicy.canAttempt(current, present, ready)) {
            return VoiceOutputPolicy.resolve(current, present, ready, false);
        }

        TtsRequest request = VillagerSpeech.fromReply(text, playerLanguage, current.volume(), pitchHz);
        short[] pcm = tts.synthesize(request);
        boolean played = pcm.length > 0 && voiceBackend.play(new VoicePlaybackRequest(
                entityId,
                entity,
                pcm,
                (float) current.effectiveRange(responseRadius)
        ));
        return VoiceOutputPolicy.resolve(current, present, ready, played);
    }
}
