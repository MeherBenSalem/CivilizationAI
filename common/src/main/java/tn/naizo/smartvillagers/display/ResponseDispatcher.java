package tn.naizo.smartvillagers.display;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import tn.naizo.smartvillagers.DisplayMode;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.villager.VillagerPersona;

public final class ResponseDispatcher {
    public void dispatch(ServerLevel level, ServerPlayer player, Villager villager, VillagerPersona persona, String text) {
        Component message = format(persona, text);
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();

        if (config.displayMode() == DisplayMode.ACTION_BAR) {
            player.displayClientMessage(message, true);
            return;
        }

        ProximityBroadcaster.broadcastNear(
                level,
                villager.getX(),
                villager.getY(),
                villager.getZ(),
                message
        );
    }

    public static Component format(VillagerPersona persona, String text) {
        String profession = capitalize(persona.professionLabel());
        return Component.literal("[" + profession + "] " + persona.displayName() + ": " + text);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "Villager";
        }
        if (value.length() == 1) {
            return value.toUpperCase();
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
