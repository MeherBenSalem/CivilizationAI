package tn.naizo.smartvillagers.village;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.platform.Mc;
import tn.naizo.smartvillagers.villager.VillagerAiData;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Village requests: villagers ask players for favours in chat, track them, pay emeralds and add positive vanilla
 * gossip when they are done, and tell their neighbours. Works the same with or without an AI provider: the AI only
 * words the villager's reply.
 */
public final class RequestService {
    /** An unanswered offer is withdrawn after two in-game minutes. */
    static final long OFFER_TIMEOUT_TICKS = 2400;
    /** Neighbours within this many blocks hear about a finished request. */
    static final double NEWS_RADIUS = 48.0;

    private final VillageLedger ledger = new VillageLedger();
    private final Map<UUID, UUID> lastOffer = new ConcurrentHashMap<>();

    public VillageLedger ledger() {
        return ledger;
    }

    private MinecraftServer loadedFor;

    /** Loads the world's ledger the first time a server ticks (and again for a new singleplayer world). */
    public void ensureLoaded(MinecraftServer server) {
        if (loadedFor != server) {
            loadedFor = server;
            lastOffer.clear();
            ledger.load(Mc.worldDirectory(server).resolve(Constants.MOD_ID).resolve("ledger.json"));
        }
    }

    public void unload() {
        loadedFor = null;
        lastOffer.clear();
    }

    public Standing standing(ServerPlayer player, Villager villager) {
        return Standing.of(villager.getPlayerReputation(player), ledger.favours(player.getUUID()));
    }

    /** Works out what a chat message means for the requests between this player and villager, and applies it. */
    public RequestEvent onChat(ServerPlayer player, Villager villager, VillagerPersona persona, String message) {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        if (!config.requestsEnabled()) {
            return RequestEvent.NONE;
        }
        VillagerTies ties = VillagerAiData.get(villager).ties();
        UUID id = player.getUUID();
        long now = villager.level().getGameTime();
        Optional<VillageRequest> current = ties.request(id);
        if (current.isPresent()) {
            VillageRequest request = current.get();
            if (request.accepted()) {
                int have = measure(player, request);
                if (request.isMet(have)) {
                    return complete(player, villager, persona, request);
                }
                if (RequestIntent.abandons(message)) {
                    ties.setRequest(id, null);
                    ledger.untrack(id, villager.getUUID());
                    return RequestEvent.of(RequestEvent.Type.ABANDONED, request, request.progress(have));
                }
                boolean asks = RequestIntent.asksForWork(message) || RequestIntent.reportsProgress(message);
                return asks ? RequestEvent.of(RequestEvent.Type.PROGRESS, request, request.progress(have)) : RequestEvent.NONE;
            }
            if (now - request.createdAt() > OFFER_TIMEOUT_TICKS) {
                ties.setRequest(id, null);
            } else if (RequestIntent.accepts(message)) {
                return accept(player, villager, persona);
            } else if (RequestIntent.declines(message)) {
                return decline(player, villager);
            } else if (RequestIntent.asksForWork(message)) {
                lastOffer.put(id, villager.getUUID());
                return RequestEvent.of(RequestEvent.Type.OFFERED, request, 0);
            } else {
                return RequestEvent.NONE;
            }
        }
        return RequestIntent.asksForWork(message) ? offer(player, villager, persona, null) : RequestEvent.NONE;
    }

    /** Offers a new request (a kind of {@code null} lets the catalogue choose). */
    public RequestEvent offer(ServerPlayer player, Villager villager, VillagerPersona persona, RequestKind forced) {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        VillagerTies ties = VillagerAiData.get(villager).ties();
        UUID id = player.getUUID();
        long now = villager.level().getGameTime();
        if (forced == null) {
            if (!RequestCatalog.offersWork(persona.professionKey())) {
                return RequestEvent.of(RequestEvent.Type.NO_TRADE, null, 0);
            }
            if (!standing(player, villager).givesWork()) {
                return RequestEvent.of(RequestEvent.Type.REFUSED, null, 0);
            }
            if (now < ties.nextRequestAt(id)) {
                return RequestEvent.of(RequestEvent.Type.COOLDOWN, null, 0);
            }
            if (ledger.active(id).size() >= config.maxActiveRequests()) {
                return RequestEvent.of(RequestEvent.Type.TOO_MANY, null, 0);
            }
        }
        // An admin-assigned request replaces whatever this villager had asked the player before.
        ties.request(id).filter(VillageRequest::accepted).ifPresent(old -> ledger.untrack(id, villager.getUUID()));
        long seed = villager.getUUID().getLeastSignificantBits() ^ id.getMostSignificantBits() ^ (now * 31L)
                ^ ledger.favours(id);
        String profession = RequestCatalog.offersWork(persona.professionKey()) ? persona.professionKey() : "minecraft:farmer";
        Optional<VillageRequest> rolled = Optional.empty();
        for (int attempt = 0; attempt < 16 && rolled.isEmpty(); attempt++) {
            rolled = RequestCatalog.roll(profession, Mc.villagerLevel(villager), seed + attempt, now)
                    .filter(r -> forced == null || r.kind() == forced)
                    .filter(this::resolvable);
        }
        if (rolled.isEmpty()) {
            return RequestEvent.NONE;
        }
        ties.setRequest(id, rolled.get());
        lastOffer.put(id, villager.getUUID());
        return RequestEvent.of(RequestEvent.Type.OFFERED, rolled.get(), 0);
    }

    public RequestEvent accept(ServerPlayer player, Villager villager, VillagerPersona persona) {
        VillagerTies ties = VillagerAiData.get(villager).ties();
        Optional<VillageRequest> offered = ties.request(player.getUUID()).filter(r -> !r.accepted());
        if (offered.isEmpty()) {
            return RequestEvent.NONE;
        }
        VillageRequest request = offered.get();
        int baseline = request.kind() == RequestKind.SLAY ? measure(player, request) : 0;
        VillageRequest accepted = request.accept(baseline);
        ties.setRequest(player.getUUID(), accepted);
        ledger.track(player.getUUID(), new VillageLedger.Entry(villager.getUUID(), persona.displayName(),
                persona.professionLabel(), accepted));
        lastOffer.remove(player.getUUID());
        return RequestEvent.of(RequestEvent.Type.ACCEPTED, accepted,
                accepted.progress(measure(player, accepted)));
    }

    public RequestEvent decline(ServerPlayer player, Villager villager) {
        VillagerTies ties = VillagerAiData.get(villager).ties();
        Optional<VillageRequest> offered = ties.request(player.getUUID()).filter(r -> !r.accepted());
        if (offered.isEmpty()) {
            return RequestEvent.NONE;
        }
        ties.setRequest(player.getUUID(), null);
        lastOffer.remove(player.getUUID());
        return RequestEvent.of(RequestEvent.Type.DECLINED, offered.get(), 0);
    }

    /** A villager died: its accepted requests no longer count towards anyone's open requests. */
    public void onVillagerGone(Villager villager) {
        VillagerAiData.get(villager).ties().requests().forEach((player, request) -> {
            if (request.accepted()) {
                ledger.untrack(player, villager.getUUID());
            }
        });
    }

    /** Forgets a villager's requests (memory clear). */
    public void forget(Villager villager) {
        onVillagerGone(villager);
        VillagerAiData.get(villager).ties().clear();
    }

    /** The villager whose offer the player saw last (for the clickable Accept and Decline buttons). */
    public Optional<Villager> lastOfferFrom(ServerPlayer player) {
        UUID villager = lastOffer.get(player.getUUID());
        if (villager == null) {
            return Optional.empty();
        }
        Entity entity = Mc.level(player).getEntity(villager);
        return entity instanceof Villager v && v.isAlive() ? Optional.of(v) : Optional.empty();
    }

    /** Items carried (FETCH) or the kill statistic (SLAY) for the request's target. */
    public int measure(ServerPlayer player, VillageRequest request) {
        if (request.kind() == RequestKind.SLAY) {
            return Mc.entityType(request.target()).map(type -> Mc.kills(player, type)).orElse(0);
        }
        Optional<Item> item = Mc.item(request.target());
        if (item.isEmpty()) {
            return 0;
        }
        int count = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean resolvable(VillageRequest request) {
        return request.kind() == RequestKind.SLAY ? Mc.entityType(request.target()).isPresent()
                : Mc.item(request.target()).isPresent();
    }

    private RequestEvent complete(ServerPlayer player, Villager villager, VillagerPersona persona, VillageRequest request) {
        if (request.kind() == RequestKind.FETCH && !take(player, request)) {
            return RequestEvent.of(RequestEvent.Type.PROGRESS, request, request.progress(measure(player, request)));
        }
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        UUID id = player.getUUID();
        VillagerTies ties = VillagerAiData.get(villager).ties();
        ties.setRequest(id, null);
        ties.setNextRequestAt(id, villager.level().getGameTime() + config.requestCooldownMinutes() * 1200L);
        ledger.untrack(id, villager.getUUID());

        Mc.give(player, new ItemStack(Items.EMERALD, request.reward()));
        player.giveExperiencePoints(3 + 2 * request.reward());
        Mc.addPositiveGossip(villager, player, config.requestReputation());
        Mc.celebrate(villager);

        int favours = ledger.recordFavour(id);
        Mc.award(player, "first_request");
        if (favours >= VillageLedger.HERO_FAVOURS) {
            Mc.award(player, "village_hero");
        }
        spreadNews(villager, id, RequestDialogue.news(request, persona.displayName(), persona.professionLabel()));
        return RequestEvent.of(RequestEvent.Type.COMPLETED, request, request.count());
    }

    private boolean take(ServerPlayer player, VillageRequest request) {
        Optional<Item> item = Mc.item(request.target());
        if (item.isEmpty() || measure(player, request) < request.count()) {
            return false;
        }
        int left = request.count();
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item.get())) {
                int used = Math.min(left, stack.getCount());
                stack.shrink(used);
                left -= used;
            }
        }
        inventory.setChanged();
        return left == 0;
    }

    private static void spreadNews(Villager giver, UUID player, String news) {
        if (!(giver.level() instanceof ServerLevel level)) {
            return;
        }
        List<Villager> neighbours = level.getEntitiesOfClass(Villager.class,
                giver.getBoundingBox().inflate(NEWS_RADIUS), v -> v.isAlive() && v != giver);
        for (Villager neighbour : neighbours) {
            VillagerAiData.get(neighbour).ties().hear(player, news);
        }
    }

    /** The system line under the villager's reply: the request's terms, progress or reward. */
    public Optional<Component> card(RequestEvent event, VillagerPersona persona) {
        VillageRequest request = event.request();
        if (request == null) {
            return Optional.empty();
        }
        return switch (event.type()) {
            case OFFERED -> Optional.of(Component.literal("Request from " + persona.displayName() + ": ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(verb(request).append(counted(request)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  Reward: " + request.reward() + " emeralds  ").withStyle(ChatFormatting.GREEN))
                    .append(Mc.button("Accept", ChatFormatting.GREEN, "/villagerai request accept",
                            "Accept " + persona.displayName() + "'s request"))
                    .append(Component.literal(" "))
                    .append(Mc.button("Decline", ChatFormatting.RED, "/villagerai request decline",
                            "Decline the request")));
            case ACCEPTED -> Optional.of(Component.literal("Request accepted: ").withStyle(ChatFormatting.GOLD)
                    .append(verb(request).append(counted(request)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + event.progress() + "/" + request.count() + "). Talk to "
                            + persona.displayName() + " when it's done. /villagerai requests lists your requests.")
                            .withStyle(ChatFormatting.GRAY)));
            case PROGRESS -> Optional.of(Component.literal(persona.displayName() + "'s request: ").withStyle(ChatFormatting.GOLD)
                    .append(verb(request).append(counted(request)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" " + event.progress() + "/" + request.count()).withStyle(ChatFormatting.YELLOW)));
            case ABANDONED -> Optional.of(Component.literal("Request dropped: ").withStyle(ChatFormatting.GRAY)
                    .append(verb(request).append(counted(request)).withStyle(ChatFormatting.WHITE)));
            case COMPLETED -> Optional.of(Component.literal("Request complete! ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .append(Component.literal("+" + request.reward() + " emeralds. " + persona.displayName()
                            + " will tell the village.").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(false))));
            default -> Optional.empty();
        };
    }

    private static MutableComponent verb(VillageRequest request) {
        return Component.literal(request.kind() == RequestKind.SLAY ? "Defeat " : "Bring ");
    }

    /** "Wheat ×16", "Zombie ×6": the game's own (translated) name and the count. */
    private static MutableComponent counted(VillageRequest request) {
        return target(request).append(Component.literal(" \u00d7" + request.count()));
    }

    private static MutableComponent target(VillageRequest request) {
        if (request.kind() == RequestKind.SLAY) {
            return Mc.entityType(request.target()).map(Mc::entityName).map(Component::copy)
                    .orElse(Component.literal(request.targetLabel()));
        }
        return Mc.item(request.target()).map(Mc::itemName).map(Component::copy)
                .orElse(Component.literal(request.targetLabel()));
    }

    /** Lines for /villagerai requests. */
    public List<Component> describeActive(ServerPlayer player) {
        return ledger.active(player.getUUID()).stream().map(entry -> {
            VillageRequest request = entry.request();
            int progress = request.progress(measure(player, request));
            return (Component) Component.literal(entry.villagerName() + " (" + entry.profession() + "): ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(verb(request).append(counted(request)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" " + progress + "/" + request.count() + ", reward " + request.reward()
                            + " emeralds").withStyle(progress >= request.count() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }).toList();
    }
}
