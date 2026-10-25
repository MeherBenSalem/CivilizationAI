package tn.naizo.smartvillagers.villager;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

public final class VillagerIdentity {
    private VillagerIdentity() {
    }

    public static VillagerPersona applyVisibleIdentity(Villager villager) {
        VillagerPersona persona = VillagerPersona.from(villager);
        if (villager.hasCustomName()) {
            return persona;
        }
        String name = VillagerPersona.visibleName(persona);
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(true);
        VillagerAiData data = VillagerAiData.get(villager);
        data.setPersonaOverride(data.personaOverride().withName(name));
        return VillagerPersona.from(villager);
    }
}
