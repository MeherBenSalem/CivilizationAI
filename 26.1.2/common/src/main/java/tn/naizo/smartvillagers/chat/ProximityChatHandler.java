package tn.naizo.smartvillagers.chat;

import net.minecraft.server.level.ServerPlayer;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;

/**
 * Entry point for proximity chat. Returns {@code true} when vanilla global chat should be cancelled.
 */
public final class ProximityChatHandler {
    private final ConversationService conversations;

    public ProximityChatHandler(ConversationService conversations) {
        this.conversations = conversations;
    }

    public boolean onChat(ServerPlayer player, String rawMessage) {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        if (!config.proximityEnabled()) {
            return false;
        }

        boolean handled = conversations.handleChat(player, rawMessage);
        return handled && config.cancelGlobalChatWhenTalking();
    }
}
