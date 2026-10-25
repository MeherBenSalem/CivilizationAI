package tn.naizo.smartvillagers.villager;

import java.util.List;

public record VillagerContext(
        String worldFacts,
        VillagerPersona persona,
        List<VillagerMemory.Exchange> recentHistory,
        int rapport,
        String professionKey,
        String playerLanguage,
        String relationship,
        String requestFact
) {
    public VillagerContext {
        playerLanguage = playerLanguage == null ? "" : playerLanguage;
        relationship = relationship == null ? "" : relationship;
        requestFact = requestFact == null ? "" : requestFact;
    }
}
