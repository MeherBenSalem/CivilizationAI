package tn.naizo.smartvillagers.villager;

import java.util.List;

public record VillagerContext(
        String worldFacts,
        VillagerPersona persona,
        List<VillagerMemory.Exchange> recentHistory,
        int rapport,
        String professionKey
) {
}
