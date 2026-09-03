package tn.naizo.smartvillagers.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import tn.naizo.smartvillagers.ActivationMode;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.ai.AiProvider;
import tn.naizo.smartvillagers.ai.AiRequest;
import tn.naizo.smartvillagers.ai.AiResponse;
import tn.naizo.smartvillagers.ai.DeepSeekProvider;
import tn.naizo.smartvillagers.ai.FallbackDialogue;
import tn.naizo.smartvillagers.ai.RateLimiter;
import tn.naizo.smartvillagers.config.ApiCredentials;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.display.ResponseDispatcher;
import tn.naizo.smartvillagers.display.ThinkingIndicator;
import tn.naizo.smartvillagers.privacy.PrivacyManager;
import tn.naizo.smartvillagers.schedule.PendingResponseQueue;
import tn.naizo.smartvillagers.session.ConversationLockManager;
import tn.naizo.smartvillagers.session.ConversationSessionManager;
import tn.naizo.smartvillagers.villager.VillagerAiData;
import tn.naizo.smartvillagers.villager.VillagerAvailability;
import tn.naizo.smartvillagers.villager.VillagerContext;
import tn.naizo.smartvillagers.villager.VillagerContextBuilder;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class ConversationService {
    private final AiProvider provider;
    private final RateLimiter rateLimiter = new RateLimiter();
    private final ConversationSessionManager sessions = new ConversationSessionManager();
    private final ConversationLockManager locks = new ConversationLockManager();
    private final PendingResponseQueue pendingResponses = new PendingResponseQueue();
    private final ResponseDispatcher dispatcher = new ResponseDispatcher();
    private final PrivacyManager privacy = new PrivacyManager();

    public ConversationService() {
        this(new DeepSeekProvider());
    }

    public ConversationService(AiProvider provider) {
        this.provider = provider;
    }

    public AiProvider provider() {
        return provider;
    }

    public PrivacyManager privacy() {
        return privacy;
    }

    public ConversationSessionManager sessions() {
        return sessions;
    }

    public ConversationLockManager locks() {
        return locks;
    }

    public PendingResponseQueue pendingResponses() {
        return pendingResponses;
    }

    public RateLimiter rateLimiter() {
        return rateLimiter;
    }

    public void boot() {
        privacy.load();
        ApiCredentials.reload();
    }

    public void shutdown() {
        pendingResponses.clear();
        sessions.clear();
        locks.clear();
        privacy.save();
    }

    public void tick(MinecraftServer server) {
        sessions.pruneInactive(120_000L);
        for (ServerLevel level : server.getAllLevels()) {
            pendingResponses.tick(level, dispatcher, sessions, locks);
        }
    }

    public boolean handleChat(ServerPlayer player, String rawMessage) {
        MessageSanitizer.Sanitized sanitized = MessageSanitizer.sanitize(rawMessage);
        if (!sanitized.valid()) {
            return false;
        }

        String message = ActivationDetector.stripPrefix(sanitized.text());
        if (message.isBlank()) {
            return false;
        }

        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        ActivationMode mode = config.activationMode();

        List<VillagerCandidate> candidates = VillagerSelector.findCandidates(player, message, MessageSignals.analyze(message, null), sessions);
        if (candidates.isEmpty()) {
            return false;
        }

        VillagerCandidate best = null;
        MessageSignals bestSignals = null;
        for (VillagerCandidate candidate : candidates) {
            MessageSignals signals = MessageSignals.analyze(message, candidate.persona().displayName());
            boolean looking = VillagerSelector.isLookingAt(player, candidate.villager());
            if (ActivationDetector.isActivated(mode, sanitized.text(), signals, looking)) {
                best = candidate;
                bestSignals = signals;
                break;
            }
        }

        if (best == null) {
            return false;
        }

        respond(player, best, message, bestSignals);
        return true;
    }

    private void respond(ServerPlayer player, VillagerCandidate candidate, String message, MessageSignals signals) {
        Villager villager = candidate.villager();
        VillagerPersona persona = candidate.persona();

        Optional<String> busy = VillagerAvailability.busyReply(villager);
        if (busy.isPresent()) {
            dispatcher.dispatch(player.level(), player, villager, persona, busy.get());
            return;
        }

        if (!locks.tryLock(villager.getUUID(), player.getUUID())) {
            player.sendSystemMessage(Component.literal(persona.displayName() + " is already talking to someone."));
            return;
        }

        sessions.start(player.getUUID(), villager.getUUID());

        if (SmartVillagersConfig.get().requirePlayerOptIn() && !privacy.hasConsent(player)) {
            String reply = FallbackDialogue.reply(persona, signals);
            scheduleReply(player, villager, message, reply, 10);
            player.sendSystemMessage(Component.literal(
                    "AI replies need consent. Use /villagerai consent accept (fallback reply used for now)."));
            return;
        }

        ThinkingIndicator.show(player, persona);

        if (!provider.isConfigured() || !rateLimiter.tryAcquire(player.getUUID())) {
            String reply = FallbackDialogue.reply(persona, signals);
            scheduleReply(player, villager, message, reply, thinkingDelay());
            return;
        }

        VillagerAiData data = VillagerAiData.get(villager);
        VillagerContext context = VillagerContextBuilder.from(player, villager, data.memory(), persona);
        AiRequest request = new AiRequest(context, message);

        MinecraftServer server = player.level().getServer();
        provider.complete(request).whenComplete((AiResponse response, Throwable error) -> {
            rateLimiter.release();
            String reply;
            if (error != null || response == null || !response.ok()) {
                Constants.LOG.debug("AI failed, using fallback: {}",
                        error != null ? error.toString() : (response != null ? response.error() : "null"));
                reply = FallbackDialogue.reply(persona, signals);
            } else {
                MessageSanitizer.Sanitized cleaned = MessageSanitizer.sanitize(response.text());
                reply = cleaned.valid() ? cleaned.text() : FallbackDialogue.reply(persona, signals);
            }

            String finalReply = reply;
            if (server != null) {
                server.execute(() -> scheduleReply(player, villager, message, finalReply, thinkingDelay()));
            } else {
                locks.unlock(villager.getUUID(), player.getUUID());
            }
        });
    }

    private void scheduleReply(ServerPlayer player, Villager villager, String playerMessage, String reply, int delayTicks) {
        pendingResponses.enqueue(new PendingResponseQueue.PendingResponse(
                player.getUUID(),
                villager.getUUID(),
                playerMessage,
                reply,
                Math.max(1, delayTicks),
                null
        ));
    }

    private static int thinkingDelay() {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        int min = Math.max(1, config.thinkingDelayMinTicks());
        int max = Math.max(min, config.thinkingDelayMaxTicks());
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
