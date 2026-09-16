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

        short[] pcm = render(text, playerLanguage, pitchHz);
        boolean played = play(entityId, entity, pcm, responseRadius);
        return VoiceOutputPolicy.resolve(current, present, ready, played);
    }

    public short[] render(String text, String playerLanguage, int pitchHz) {
        VoiceSettings current = settings.get();
        TtsRequest request = VillagerSpeech.fromReply(text, playerLanguage, current.volume(), pitchHz);
        return tts.synthesize(request);
    }

    public boolean play(UUID entityId, Object entity, short[] pcm, double responseRadius) {
        VoiceBackend voiceBackend = backend.get();
        if (voiceBackend == null || !voiceBackend.isReady() || pcm == null || pcm.length == 0) {
            return false;
        }
        VoiceSettings current = settings.get();
        return voiceBackend.play(new VoicePlaybackRequest(
                entityId,
                entity,
                pcm,
                (float) current.effectiveRange(responseRadius)
        ));
    }
}
