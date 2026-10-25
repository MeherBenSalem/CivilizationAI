package tn.naizo.smartvillagers.display;

import com.mojang.math.Transformation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.mixin.DisplayInvoker;
import tn.naizo.smartvillagers.mixin.TextDisplayInvoker;
import tn.naizo.smartvillagers.platform.Mc;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Speech bubbles: a vanilla text display riding the villager, so every player sees the reply above its head
 * without a client mod. Bubbles live for a few seconds and are never left behind (see {@link #isOrphan}).
 */
public final class SpeechBubbles {
    public static final String TAG = "smartvillagers.bubble";
    private static final Map<UUID, Bubble> LIVE = new ConcurrentHashMap<>();
    private static long serverTicks;

    private record Bubble(Display.TextDisplay entity, long expiresAt) {
    }

    private SpeechBubbles() {
    }

    /** "..." under the villager's name while it waits for the AI provider. */
    public static void thinking(Villager villager, String speaker) {
        show(villager, bubbleText(speaker, "...", ChatFormatting.GRAY), 30 * 20);
    }

    public static void say(Villager villager, String speaker, String text) {
        show(villager, bubbleText(speaker, text, ChatFormatting.WHITE), durationTicks(text));
    }

    private static Component bubbleText(String speaker, String text, ChatFormatting color) {
        return Component.literal(speaker).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\n" + text).withStyle(style -> style.withColor(color).withBold(false)));
    }

    /** Time to read it: about 2.5 words per second plus 3 seconds, between 5 and 15 seconds. */
    static int durationTicks(String text) {
        int words = text == null || text.isBlank() ? 1 : text.trim().split("\\s+").length;
        return Math.max(5 * 20, Math.min(15 * 20, 60 + words * 8));
    }

    private static void show(Villager villager, Component content, int ticks) {
        if (!SmartVillagersConfig.get().speechBubbles() || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        Bubble existing = LIVE.get(villager.getUUID());
        Display.TextDisplay bubble = existing != null && existing.entity().isAlive() ? existing.entity() : null;
        if (bubble == null) {
            bubble = Mc.newTextDisplay(level);
            bubble.addTag(TAG);
            bubble.setPos(villager.getX(), villager.getY() + villager.getBbHeight(), villager.getZ());
            ((DisplayInvoker) bubble).smartVillagers$setBillboardConstraints(Display.BillboardConstraints.CENTER);
            // Above the name tag (the bubble repeats the name in gold).
            ((DisplayInvoker) bubble).smartVillagers$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.8F, 0.0F), new Quaternionf(), new Vector3f(0.6F, 0.6F, 0.6F), new Quaternionf()));
            ((TextDisplayInvoker) bubble).smartVillagers$setLineWidth(170);
            ((TextDisplayInvoker) bubble).smartVillagers$setBackgroundColor(0xD0281C12);
            ((TextDisplayInvoker) bubble).smartVillagers$setText(content);
            if (!level.addFreshEntity(bubble)) {
                return;
            }
            if (!Mc.ride(bubble, villager)) {
                bubble.discard();
                return;
            }
        } else {
            ((TextDisplayInvoker) bubble).smartVillagers$setText(content);
        }
        LIVE.put(villager.getUUID(), new Bubble(bubble, serverTicks + ticks));
    }

    public static void clear(Villager villager) {
        Bubble bubble = LIVE.remove(villager.getUUID());
        if (bubble != null) {
            bubble.entity().discard();
        }
    }

    public static void tick(MinecraftServer server) {
        serverTicks++;
        Iterator<Map.Entry<UUID, Bubble>> it = LIVE.entrySet().iterator();
        while (it.hasNext()) {
            Bubble bubble = it.next().getValue();
            if (serverTicks >= bubble.expiresAt() || !bubble.entity().isAlive() || bubble.entity().getVehicle() == null) {
                bubble.entity().discard();
                it.remove();
            }
        }
    }

    public static void shutdown() {
        LIVE.values().forEach(bubble -> bubble.entity().discard());
        LIVE.clear();
    }

    public static int live() {
        return LIVE.size();
    }

    /** A bubble this session did not create (saved with its villager before a crash): remove it. */
    public static boolean isOrphan(Entity passenger) {
        if (!Mc.hasTag(passenger, TAG)) {
            return false;
        }
        return LIVE.values().stream().noneMatch(bubble -> bubble.entity() == passenger);
    }
}
