package tn.naizo.smartvillagers.forge;

import net.minecraftforge.fml.common.Mod;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.SmartVillagers;

@Mod(Constants.MOD_ID)
public final class SmartVillagersForge {
    public SmartVillagersForge() {
        SmartVillagers.initialize();
    }
}
