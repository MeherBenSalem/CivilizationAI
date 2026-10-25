package tn.naizo.smartvillagers.platform;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import tn.naizo.smartvillagers.Constants;

import java.nio.file.Path;
import java.util.Optional;

/** The Minecraft calls whose names or shapes differ between versions. One copy per workspace (this one: 26.2 and 26.3). */
public final class Mc {
    private Mc() {
    }

    public static boolean isOp(net.minecraft.commands.CommandSourceStack source) {
        return net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(source);
    }

    public static String playerName(ServerPlayer player) {
        return player.getGameProfile().name();
    }

    /** The client's language code (en_us, fr_fr...) or "" when unknown. */
    public static String language(ServerPlayer player) {
        try {
            var information = player.clientInformation();
            String language = information == null ? null : information.language();
            return language == null ? "" : language;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public static String dimension(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    /** Ticks since dawn, 0 to 23999. */
    public static long timeOfDay(ServerLevel level) {
        return Math.floorMod(level.getOverworldClockTime(), 24000L);
    }

    public static boolean isDay(ServerLevel level) {
        return level.isBrightOutside();
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.level();
    }

    public static MinecraftServer server(ServerPlayer player) {
        return player.level().getServer();
    }

    public static Optional<Item> item(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(key).filter(item -> item != Items.AIR);
    }

    public static Optional<EntityType<?>> entityType(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
            return Optional.empty();
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(key).map(type -> (EntityType<?>) type);
    }

    public static Component itemName(Item item) {
        return Component.translatable(item.getDescriptionId());
    }

    public static Component entityName(EntityType<?> type) {
        return type.getDescription();
    }

    public static int kills(ServerPlayer player, EntityType<?> type) {
        return player.getStats().getValue(Stats.ENTITY_KILLED.get(type));
    }

    public static void addPositiveGossip(Villager villager, ServerPlayer player, int amount) {
        if (amount > 0) {
            villager.getGossips().add(player.getUUID(), GossipType.MINOR_POSITIVE, amount);
        }
    }

    public static int villagerLevel(Villager villager) {
        return villager.getVillagerData().level();
    }

    public static String biome(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> key.identifier().getPath().replace('_', ' ')).orElse("");
    }

    public static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static Path worldDirectory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT);
    }

    /** Grants an advancement made of one {@code minecraft:impossible} criterion named "done". */
    public static void award(ServerPlayer player, String path) {
        MinecraftServer server = server(player);
        if (server == null) {
            return;
        }
        AdvancementHolder holder = server.getAdvancements()
                .get(Identifier.fromNamespaceAndPath(Constants.MOD_ID, path));
        if (holder != null) {
            player.getAdvancements().award(holder, "done");
        }
    }

    public static boolean hasAdvancement(ServerPlayer player, String path) {
        MinecraftServer server = server(player);
        AdvancementHolder holder = server == null ? null : server.getAdvancements()
                .get(Identifier.fromNamespaceAndPath(Constants.MOD_ID, path));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    public static MutableComponent button(String label, ChatFormatting color, String command, String hover) {
        return Component.literal("[" + label + "]").withStyle(style -> style.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    /** Happy-villager particles and a celebration sound over a villager. */
    public static void celebrate(Villager villager) {
        if (villager.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.8, villager.getZ(),
                    14, 0.5, 0.5, 0.5, 0.0);
            level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    /** Into the inventory, or dropped at the player's feet when it is full. */
    public static void give(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack);
    }

    public static void actionBar(ServerPlayer player, Component message) {
        player.sendSystemMessage(message, true);
    }

    public static Display.TextDisplay newTextDisplay(ServerLevel level) {
        return new Display.TextDisplay(net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY, level);
    }

    public static boolean hasTag(Entity entity, String tag) {
        return entity.entityTags().contains(tag);
    }

    public static boolean ride(Entity passenger, Entity vehicle) {
        return passenger.startRiding(vehicle, true, false);
    }
}
