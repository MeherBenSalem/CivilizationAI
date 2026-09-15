package tn.naizo.smartvillagers.voice.svc;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.voice.VoiceOutput;

/**
 * Loaded by Simple Voice Chat (Fabric entrypoint {@code voicechat} / Forge annotation).
 * Never referenced from common init, so missing SVC cannot crash the mod.
 */
@ForgeVoicechatPlugin
public final class SimpleVoiceChatPlugin implements VoicechatPlugin {
    public static final String CATEGORY_ID = "smartvillagers";

    @Override
    public String getPluginId() {
        return Constants.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        Constants.LOG.debug("Simple Voice Chat API initialized for {}", Constants.MOD_NAME);
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        VolumeCategory category = api.volumeCategoryBuilder()
                .setId(CATEGORY_ID)
                .setName("Smart Villagers")
                .setDescription("Spoken villager AI replies")
                .build();
        api.registerVolumeCategory(category);
        SimpleVoiceChatBackend backend = new SimpleVoiceChatBackend(api);
        VoiceOutput.setBackend(backend);
        Constants.LOG.info("Simple Voice Chat backend ready for villager speech.");
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        VoiceOutput.setBackend(null);
    }
}
