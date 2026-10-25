package tn.naizo.smartvillagers.fabric;

import net.fabricmc.api.ModInitializer;
import tn.naizo.smartvillagers.SmartVillagers;

public final class SmartVillagersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SmartVillagers.initialize();
    }
}
