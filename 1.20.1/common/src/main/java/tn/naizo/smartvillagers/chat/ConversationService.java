package tn.naizo.smartvillagers.chat;

import tn.naizo.smartvillagers.platform.Mc;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import tn.naizo.smartvillagers.ActivationMode;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.ai.AiProvider;
import tn.naizo.smartvillagers.ai.AiRequest;
import tn.naizo.smartvillagers.ai.AiResponse;
import tn.naizo.smartvillagers.ai.OpenAiCompatibleProvider;
import tn.naizo.smartvillagers.ai.FallbackDialogue;
import tn.naizo.smartvillagers.ai.RateLimiter;
import tn.naizo.smartvillagers.ai.ReplySelection;
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
import tn.naizo.smartvillagers.villager.VillagerIdentity;
import tn.naizo.smartvillagers.villager.VillagerPersona;
import tn.naizo.smartvillagers.display.SpeechBubbles;
import tn.naizo.smartvillagers.village.RequestDialogue;
import tn.naizo.smartvillagers.village.RequestEvent;
import tn.naizo.smartvillagers.village.RequestService;
import net.minecraft.server.level.ServerLevel;

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
    private final RequestService requests = new RequestService();

    public ConversationService() {
        this(new OpenAiCompatibleProvider());
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

    public RequestService requests() {
        return requests;
    }

    public void boot() {
        privacy.load();
        ApiCredentials.reload();
    }

    public void shutdown() {
        SpeechBubbles.shutdown();
        requests.ledger().save();
        requests.unload();
        pendingResponses.clear();
        sessions.clear();
        locks.clear();
        privacy.save();
    }

    public void tick(MinecraftServer server) {
        requests.ensureLoaded(server);
        SpeechBubbles.tick(server);
        sessions.pruneInactive(120_000L);
        pendingResponses.tick(server, dispatcher, sessions, locks);
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

        List<String> otherPlayers = config.ignorePlayerToPlayerChat()
                ? Mc.server(player).getPlayerList().getPlayers().stream()
                .filter(other -> other != player).map(Mc::playerName).toList()
                : List.of();

        VillagerCandidate best = null;
        MessageSignals bestSignals = null;
        for (VillagerCandidate candidate : candidates) {
            if (ChatEtiquette.addressedToAnotherPlayer(message, candidate.persona().displayName(), otherPlayers)) {
                continue;
            }
            MessageSignals signals = MessageSignals.analyze(message, candidate.persona().displayName());
            boolean looking = VillagerSelector.isAddressing(player, candidate.villager(), candidate.distance());
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
        VillagerPersona persona = VillagerIdentity.applyVisibleIdentity(villager);
        Mc.award(player, "root");

        Optional<String> busy = VillagerAvailability.busyReply(villager);
        if (busy.isPresent()) {
            dispatcher.dispatch(Mc.level(player), player, villager, persona, busy.get());
            return;
        }

        if (!locks.tryLock(villager.getUUID(), player.getUUID())) {
            player.sendSystemMessage(Component.literal(persona.displayName() + " is already talking to someone."));
            return;
        }

        sessions.start(player.getUUID(), villager.getUUID());
        RequestEvent event = requests.onChat(player, villager, persona, message);
        Runnable card = cardSender(player, persona, event);

        if (SmartVillagersConfig.get().requirePlayerOptIn() && !privacy.hasConsent(player)) {
            scheduleReply(player, villager, message, localReply(player, villager, persona, signals, event), 10, card);
            player.sendSystemMessage(Component.literal(
                    "AI replies need consent. Use /villagerai consent accept (local reply used for now)."));
            return;
        }

        ThinkingIndicator.show(player, persona);
        SpeechBubbles.thinking(villager, persona.displayName());

        if (!provider.isConfigured() || !rateLimiter.tryAcquire(player.getUUID())) {
            scheduleReply(player, villager, message, localReply(player, villager, persona, signals, event),
                    thinkingDelay(), card);
            return;
        }

        complete(player, villager, persona, message, signals, event, true, card);
    }

    /** Local dialogue: the request line when something happened to a request, small talk otherwise. */
    private String localReply(ServerPlayer player, Villager villager, VillagerPersona persona, MessageSignals signals,
                              RequestEvent event) {
        if (!event.isNone()) {
            return RequestDialogue.line(event, persona.professionKey());
        }
        List<String> heard = VillagerAiData.get(villager).ties().heard(player.getUUID());
        ServerLevel level = Mc.level(player);
        FallbackDialogue.Mood mood = new FallbackDialogue.Mood(requests.standing(player, villager), !Mc.isDay(level),
                level.isRaining(), heard.isEmpty() ? "" : heard.get(heard.size() - 1));
        return FallbackDialogue.line(persona, signals, mood);
    }

    private Runnable cardSender(ServerPlayer player, VillagerPersona persona, RequestEvent event) {
        return requests.card(event, persona).<Runnable>map(card -> () -> player.sendSystemMessage(card)).orElse(null);
    }

    /** Shows a request event right away (the clickable Accept and Decline buttons). */
    public void announce(ServerPlayer player, Villager villager, RequestEvent event) {
        VillagerPersona persona = VillagerPersona.from(villager);
        if (event.isNone()) {
            return;
        }
        dispatcher.dispatch(Mc.level(player), player, villager, persona, RequestDialogue.line(event, persona.professionKey()));
        Runnable card = cardSender(player, persona, event);
        if (card != null) {
            card.run();
        }
    }

    /**
     * Forced talk for {@code /villagerai test}: skips activation and consent.
     */
    public boolean testTalk(ServerPlayer player, Villager villager) {
        String message = "Hello! Please introduce yourself in one short sentence.";
        VillagerPersona persona = VillagerIdentity.applyVisibleIdentity(villager);
        MessageSignals signals = MessageSignals.analyze(message, persona.displayName());

        if (!locks.tryLock(villager.getUUID(), player.getUUID())) {
            player.sendSystemMessage(Component.literal(persona.displayName() + " is already talking to someone."));
            return false;
        }

        sessions.start(player.getUUID(), villager.getUUID());
        ThinkingIndicator.show(player, persona);

        if (!provider.isConfigured() || !rateLimiter.tryAcquire(player.getUUID())) {
            deliverNow(player, villager, message, localReply(player, villager, persona, signals, RequestEvent.NONE),
                    !provider.isConfigured() ? "AI provider not configured; check provider, endpoint, model and key. Fallback used." : "Rate limited; fallback used.");
            return true;
        }

        player.sendSystemMessage(Component.literal("Waiting for AI provider (up to 30s)…"));
        complete(player, villager, persona, message, signals, RequestEvent.NONE, false, null);
        return true;
    }

    private void complete(ServerPlayer player, Villager villager, VillagerPersona persona, String message,
                          MessageSignals signals, RequestEvent event, boolean delayed, Runnable card) {
        VillagerAiData data = VillagerAiData.get(villager);
        VillagerContext context = VillagerContextBuilder.from(player, villager, data.memory(), persona,
                requests.standing(player, villager), requests.ledger().favours(player.getUUID()), event);
        AiRequest request = new AiRequest(context, message);
        MinecraftServer server = Mc.server(player);
        String local = localReply(player, villager, persona, signals, event);
        provider.complete(request).whenComplete((AiResponse response, Throwable error) -> {
            rateLimiter.release();
            boolean usedFallback = error != null || response == null || !response.ok();
            if (usedFallback) {
                Constants.LOG.warn("AI failed, using local dialogue: {}",
                        error != null ? error.toString() : (response != null ? response.error() : "null"));
            }
            String reply = usedFallback ? local : ReplySelection.resolve(response, local);
            if (server != null) {
                server.execute(() -> {
                    if (delayed) {
                        scheduleReply(player, villager, message, reply, thinkingDelay(), card);
                    } else {
                        String note = usedFallback
                                ? "AI provider failed (" + (error != null ? error.toString()
                                : (response != null ? response.error() : "null")) + "); local reply used."
                                : "AI provider replied.";
                        deliverNow(player, villager, message, reply, note);
                    }
                });
            } else {
                locks.unlock(villager.getUUID(), player.getUUID());
            }
        });
    }

    private void deliverNow(ServerPlayer player, Villager villager, String playerMessage, String reply, String note) {
        VillagerPersona persona = VillagerIdentity.applyVisibleIdentity(villager);
        var voice = dispatcher.dispatch(Mc.level(player), player, villager, persona, reply);
        VillagerAiData.get(villager).memory().remember(player.getUUID(), playerMessage, reply);
        sessions.touch(player.getUUID());
        locks.unlock(villager.getUUID(), player.getUUID());
        if (note != null && !note.isBlank()) {
            player.sendSystemMessage(Component.literal(note));
        }
        player.sendSystemMessage(Component.literal("Voice spoken: " + (voice.spoken() ? "yes" : "no")));
    }

    private void scheduleReply(ServerPlayer player, Villager villager, String playerMessage, String reply, int delayTicks,
                               Runnable afterDelivery) {
        pendingResponses.enqueue(new PendingResponseQueue.PendingResponse(
                player.getUUID(),
                villager.getUUID(),
                playerMessage,
                reply,
                Math.max(1, delayTicks),
                afterDelivery
        ));
    }

    private static int thinkingDelay() {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        int min = Math.max(1, config.thinkingDelayMinTicks());
        int max = Math.max(min, config.thinkingDelayMaxTicks());
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
