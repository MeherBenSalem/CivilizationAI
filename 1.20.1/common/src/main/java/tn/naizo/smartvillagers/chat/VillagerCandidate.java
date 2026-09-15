package tn.naizo.smartvillagers.chat;

import net.minecraft.world.entity.npc.Villager;
import tn.naizo.smartvillagers.villager.VillagerPersona;

public record VillagerCandidate(Villager villager, VillagerPersona persona, double distance, double score) {
}
