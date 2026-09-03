package tn.naizo.smartvillagers.platform;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import tn.naizo.smartvillagers.platform.IPlatformHelper;

import java.nio.file.Path;

public final class ForgePlatformHelper implements IPlatformHelper {
    private final ForgePlatformEvents events = new ForgePlatformEvents();

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public IPlatformEvents events() {
        return events;
    }
}
