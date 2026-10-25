package tn.naizo.smartvillagers.platform;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ServerChatCallback {
    /**
     * @return true to cancel the vanilla chat broadcast
     */
    boolean onChat(ServerPlayer player, String message);
}
