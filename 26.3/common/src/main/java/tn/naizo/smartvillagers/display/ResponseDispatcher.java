package tn.naizo.smartvillagers.display;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import tn.naizo.smartvillagers.DisplayMode;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.villager.VillagerContextBuilder;
import tn.naizo.smartvillagers.villager.VillagerPersona;
import tn.naizo.smartvillagers.voice.VoiceDelivery;
import tn.naizo.smartvillagers.voice.VoiceOutput;

public final class ResponseDispatcher {
    public VoiceDelivery dispatch(ServerLevel level, ServerPlayer player, Villager villager, VillagerPersona persona, String text) {
        VoiceDelivery voice = VoiceOutput.speak(villager, text, VillagerContextBuilder.playerLanguage(player));
        if (!voice.showText()) {
            return voice;
        }

        Component message = format(persona, text);
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();

        if (config.displayMode() == DisplayMode.ACTION_BAR) {
            player.sendSystemMessage(message, true);
            return voice;
        }

        player.sendSystemMessage(message);
        ProximityBroadcaster.broadcastNearExcept(
                level,
                villager.getX(),
                villager.getY(),
                villager.getZ(),
                message,
                player.getUUID()
        );
        return voice;
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
