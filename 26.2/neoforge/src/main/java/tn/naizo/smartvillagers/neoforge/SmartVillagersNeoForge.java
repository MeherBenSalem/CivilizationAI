package tn.naizo.smartvillagers.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.SmartVillagers;

@Mod(Constants.MOD_ID)
public final class SmartVillagersNeoForge {
    public SmartVillagersNeoForge(IEventBus modEventBus) {
        SmartVillagers.initialize();
    }
}
