package tn.naizo.smartvillagers.chat;

import net.minecraft.world.entity.npc.villager.Villager;
import tn.naizo.smartvillagers.villager.VillagerPersona;

public record VillagerCandidate(Villager villager, VillagerPersona persona, double distance, double score) {
}
