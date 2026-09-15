package tn.naizo.smartvillagers.voice.svc;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.voice.VoiceBackend;
import tn.naizo.smartvillagers.voice.VoicePlaybackRequest;

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
            EntityAudioChannel channel = api.createEntityAudioChannel(
                    request.entityId(),
                    api.fromEntity(request.entity())
            );
            if (channel == null) {
                return false;
            }
            channel.setCategory(SimpleVoiceChatPlugin.CATEGORY_ID);
            channel.setDistance(Math.max(1.0f, request.range()));

            AudioPlayer previous = playing.remove(request.entityId());
            if (previous != null) {
                previous.stopPlaying();
            }

            AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), request.pcm());
            player.setOnStopped(() -> playing.remove(request.entityId(), player));
            playing.put(request.entityId(), player);
            player.startPlaying();
            return true;
        } catch (Throwable t) {
            Constants.LOG.debug("Simple Voice Chat rejected villager audio", t);
            return false;
        }
    }
}
