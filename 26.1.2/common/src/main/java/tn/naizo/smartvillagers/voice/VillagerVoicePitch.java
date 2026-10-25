package tn.naizo.smartvillagers.voice;

import java.util.UUID;

public final class VillagerVoicePitch {
    private VillagerVoicePitch() {
    }

    public static int hz(UUID villagerId) {
        int hash = villagerId == null ? 0 : villagerId.hashCode();
        return 115 + Math.floorMod(hash, 65);
    }
}
