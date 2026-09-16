package tn.naizo.smartvillagers.voice;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.npc.Villager;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.platform.Services;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-game facade. All SVC types live in {@code voice.svc} and are loaded only by
 * Simple Voice Chat's plugin entrypoint.
 */
public final class VoiceOutput {
    private static final AtomicReference<VoiceBackend> BACKEND = new AtomicReference<>(VoiceBackend.NO_OP);
    private static final ExecutorService TTS = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "smartvillagers-tts");
        thread.setDaemon(true);
        return thread;
    });
    private static final VoiceCoordinator COORDINATOR = new VoiceCoordinator(
            new CompositeTtsSynthesizer(),
            BACKEND::get,
            () -> VoiceSettings.from(SmartVillagersConfig.get()),
            VoiceOutput::voiceChatPresent
    );

    private VoiceOutput() {
    }

    public static void bootstrap() {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        VoiceStartupLog.emitOnce(Constants.LOG::info, config.voiceEnabled(), voiceChatPresent());
    }

    public static void setBackend(VoiceBackend backend) {
        BACKEND.set(backend == null ? VoiceBackend.NO_OP : backend);
    }

    public static boolean backendReady() {
        return BACKEND.get().isReady();
    }

    public static VoiceDelivery speak(Villager villager, String text, String playerLanguage) {
        try {
            MinecraftServer server = villager.getServer();
            boolean offload = WindowsSapiTts.available() && server != null && server.isSameThread();
            if (offload) {
                UUID id = villager.getUUID();
                int pitch = VillagerVoicePitch.hz(id);
                double radius = SmartVillagersConfig.get().responseRadius();
                TTS.execute(() -> {
                    try {
                        short[] pcm = COORDINATOR.render(text, playerLanguage, pitch);
                        server.execute(() -> {
                            boolean played = COORDINATOR.play(id, villager, pcm, radius);
                            if (SmartVillagersConfig.get().voiceEnabled() && voiceChatPresent() && backendReady()
                                    && !played) {
                                Constants.LOG.warn("Villager voice did not play. Check SVC volume category 'Smart Villagers'.");
                            }
                        });
                    } catch (Throwable t) {
                        Constants.LOG.warn("Villager voice playback failed; using text", t);
                    }
                });
                return new VoiceDelivery(true, true);
            }
            VoiceDelivery delivery = COORDINATOR.speak(
                    villager.getUUID(),
                    villager,
                    text,
                    playerLanguage,
                    VillagerVoicePitch.hz(villager.getUUID()),
                    SmartVillagersConfig.get().responseRadius()
            );
            if (SmartVillagersConfig.get().voiceEnabled() && voiceChatPresent() && backendReady()
                    && !delivery.spoken()) {
                Constants.LOG.warn("Villager voice did not play (falling back to text). Check SVC volume category 'Smart Villagers'.");
            }
            return delivery;
        } catch (Throwable t) {
            Constants.LOG.warn("Villager voice playback failed; using text", t);
            return VoiceDelivery.textOnly();
        }
    }

    static boolean voiceChatPresent() {
        try {
            return VoiceAvailability.isModLoaded(Services.PLATFORM::isModLoaded);
        } catch (Throwable t) {
            return false;
        }
    }
}
