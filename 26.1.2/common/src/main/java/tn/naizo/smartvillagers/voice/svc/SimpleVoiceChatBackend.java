package tn.naizo.smartvillagers.voice.svc;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.voice.Pcm;
import tn.naizo.smartvillagers.voice.VoiceBackend;
import tn.naizo.smartvillagers.voice.VoicePlaybackRequest;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class SimpleVoiceChatBackend implements VoiceBackend {
    private final VoicechatServerApi api;
    private final ConcurrentHashMap<UUID, AudioPlayer> playing = new ConcurrentHashMap<>();

    SimpleVoiceChatBackend(VoicechatServerApi api) {
        this.api = api;
    }

    @Override
    public boolean isReady() {
        return api != null;
    }

    @Override
    public boolean play(VoicePlaybackRequest request) {
        if (request == null || request.pcm() == null || request.pcm().length == 0 || request.entity() == null) {
            return false;
        }
        try {
            List<short[]> frames = Pcm.frames960(request.pcm());
            if (frames.isEmpty()) {
                return false;
            }
            EntityAudioChannel channel = api.createEntityAudioChannel(
                    UUID.randomUUID(),
                    api.fromEntity(request.entity())
            );
            if (channel == null) {
                Constants.LOG.warn("Simple Voice Chat refused an entity audio channel for villager {}", request.entityId());
                return false;
            }
            channel.setCategory(SimpleVoiceChatPlugin.CATEGORY_ID);
            channel.setDistance(Math.max(1.0f, request.range()));

            AudioPlayer previous = playing.remove(request.entityId());
            if (previous != null) {
                previous.stopPlaying();
            }

            Iterator<short[]> iterator = frames.iterator();
            AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), () -> {
                if (!iterator.hasNext()) {
                    return null;
                }
                return iterator.next();
            });
            player.setOnStopped(() -> playing.remove(request.entityId(), player));
            playing.put(request.entityId(), player);
            player.startPlaying();
            Constants.LOG.info("Playing {} villager voice samples ({} frames) via Simple Voice Chat",
                    request.pcm().length, frames.size());
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("Simple Voice Chat rejected villager audio", t);
            return false;
        }
    }
}
