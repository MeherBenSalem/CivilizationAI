package tn.naizo.smartvillagers.schedule;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.display.ResponseDispatcher;
import tn.naizo.smartvillagers.session.ConversationLockManager;
import tn.naizo.smartvillagers.session.ConversationSessionManager;
import tn.naizo.smartvillagers.villager.VillagerAiData;
import tn.naizo.smartvillagers.villager.VillagerPersona;
import tn.naizo.smartvillagers.voice.VoiceDelivery;

import java.util.UUID;

public final class PendingResponseQueue {
    private final ReplyDelayQueue delays = new ReplyDelayQueue();

    public void enqueue(PendingResponse response) {
        delays.enqueue(response.task);
    }

    public void tick(MinecraftServer server, ResponseDispatcher dispatcher, ConversationSessionManager sessions,
                     ConversationLockManager locks) {
        for (ReplyDelayQueue.Task task : delays.takeReady()) {
            PendingResponse.deliver(task, server, dispatcher, sessions, locks);
        }
    }

    public void clear() {
        delays.clear();
    }

    public int size() {
        return delays.size();
    }

    public static final class PendingResponse {
        private final ReplyDelayQueue.Task task;

        public PendingResponse(UUID playerId, UUID villagerId, String playerMessage, String replyText,
                               int remainingTicks, java.util.function.Consumer<String> ignored) {
            this.task = new ReplyDelayQueue.Task(playerId, villagerId, playerMessage, replyText, remainingTicks);
        }

        static void deliver(ReplyDelayQueue.Task task, MinecraftServer server, ResponseDispatcher dispatcher,
                            ConversationSessionManager sessions, ConversationLockManager locks) {
            ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
            Villager villager = findVillager(server, task.villagerId);
            if (player == null || villager == null || !villager.isAlive()) {
                Constants.LOG.warn("Dropped villager reply (player present={}, villager present={})",
                        player != null, villager != null);
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "Villager reply was lost (villager not found in this world)."));
                }
                locks.unlock(task.villagerId, task.playerId);
                return;
            }

            VillagerPersona persona = VillagerPersona.from(villager);
            ServerLevel level = player.serverLevel();
            VoiceDelivery voice = dispatcher.dispatch(level, player, villager, persona, task.replyText);
            VillagerAiData.get(villager).memory().remember(task.playerId, task.playerMessage, task.replyText);
            sessions.touch(task.playerId);
            locks.unlock(task.villagerId, task.playerId);
        }

        private static Villager findVillager(MinecraftServer server, UUID id) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(id);
                if (entity instanceof Villager villager) {
                    return villager;
                }
            }
            return null;
        }
    }
}
