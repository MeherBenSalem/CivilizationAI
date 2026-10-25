package tn.naizo.smartvillagers.voice;

import java.util.UUID;

public record VoicePlaybackRequest(UUID entityId, Object entity, short[] pcm, float range) {
}
