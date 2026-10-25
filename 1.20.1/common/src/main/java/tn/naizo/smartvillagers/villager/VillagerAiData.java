package tn.naizo.smartvillagers.villager;

import net.minecraft.world.entity.npc.Villager;
import tn.naizo.smartvillagers.village.VillagerTies;

public final class VillagerAiData {
    private VillagerMemory memory = new VillagerMemory();
    private PersonaOverride personaOverride = PersonaOverride.EMPTY;
    private VillagerTies ties = new VillagerTies();

    /** Requests, neighbourly news and request cooldowns per player. */
    public VillagerTies ties() {
        return ties;
    }

    public void setTies(VillagerTies ties) {
        this.ties = ties != null ? ties : new VillagerTies();
    }

    public VillagerMemory memory() {
        return memory;
    }

    public void setMemory(VillagerMemory memory) {
        this.memory = memory != null ? memory : new VillagerMemory();
    }

    public PersonaOverride personaOverride() {
        return personaOverride;
    }

    public void setPersonaOverride(PersonaOverride personaOverride) {
        this.personaOverride = personaOverride != null ? personaOverride : PersonaOverride.EMPTY;
    }

    public static VillagerAiData get(Villager villager) {
        if (villager instanceof VillagerAiDataHolder holder) {
            VillagerAiData data = holder.smartVillagers$getAiData();
            if (data == null) {
                data = new VillagerAiData();
                holder.smartVillagers$setAiData(data);
            }
            return data;
        }
        throw new IllegalStateException("Villager does not implement VillagerAiDataHolder");
    }
}
