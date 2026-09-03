package tn.naizo.smartvillagers.schedule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import tn.naizo.smartvillagers.display.ResponseDispatcher;
import tn.naizo.smartvillagers.session.ConversationLockManager;
import tn.naizo.smartvillagers.session.ConversationSessionManager;
import tn.naizo.smartvillagers.villager.VillagerAiData;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class PendingResponseQueue {
    private final List<PendingResponse> pending = new ArrayList<>();

    public void enqueue(PendingResponse response) {
        synchronized (pending) {
            pending.add(response);
        }
    }

    public void tick(ServerLevel level, ResponseDispatcher dispatcher, ConversationSessionManager sessions,
                     ConversationLockManager locks) {
        List<PendingResponse> ready = new ArrayList<>();
        synchronized (pending) {
            Iterator<PendingResponse> iterator = pending.iterator();
            while (iterator.hasNext()) {
                PendingResponse response = iterator.next();
                response.remainingTicks--;
                if (response.remainingTicks <= 0) {
                    ready.add(response);
                    iterator.remove();
                }
            }
        }

        for (PendingResponse response : ready) {
            response.deliver(level, dispatcher, sessions, locks);
        }
    }

    public void clear() {
        synchronized (pending) {
            pending.clear();
        }
    }

    public static final class PendingResponse {
        private final UUID playerId;
        private final UUID villagerId;
        private final String playerMessage;
        private final String replyText;
        private int remainingTicks;
        private final Consumer<String> onDelivered;

        public PendingResponse(UUID playerId, UUID villagerId, String playerMessage, String replyText,
                               int remainingTicks, Consumer<String> onDelivered) {
            this.playerId = playerId;
            this.villagerId = villagerId;
            this.playerMessage = playerMessage;
            this.replyText = replyText;
            this.remainingTicks = remainingTicks;
            this.onDelivered = onDelivered;
        }

        public void deliver(ServerLevel level, ResponseDispatcher dispatcher, ConversationSessionManager sessions,
                            ConversationLockManager locks) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            Villager villager = level.getEntity(villagerId) instanceof Villager v ? v : null;
            if (player == null || villager == null || !villager.isAlive()) {
                locks.unlock(villagerId, playerId);
                return;
            }

            VillagerPersona persona = VillagerPersona.from(villager);
            dispatcher.dispatch(level, player, villager, persona, replyText);
            VillagerAiData.get(villager).memory().remember(playerId, playerMessage, replyText);
            sessions.touch(playerId);
            locks.unlock(villagerId, playerId);
            if (onDelivered != null) {
                onDelivered.accept(replyText);
            }
        }
    }
}
