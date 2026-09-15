package tn.naizo.smartvillagers.display;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tn.naizo.smartvillagers.villager.VillagerPersona;

public final class ThinkingIndicator {
    private ThinkingIndicator() {
    }

    public static void show(ServerPlayer player, VillagerPersona persona) {
        player.sendSystemMessage(Component.literal(persona.displayName() + " is thinking..."), true);
    }
}
