package tn.naizo.smartvillagers.ai;

import tn.naizo.smartvillagers.villager.VillagerContext;

public record AiRequest(VillagerContext context, String playerMessage) {
}
