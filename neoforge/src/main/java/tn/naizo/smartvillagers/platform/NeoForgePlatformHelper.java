package tn.naizo.smartvillagers.platform;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import tn.naizo.smartvillagers.platform.IPlatformHelper;

import java.nio.file.Path;

public final class NeoForgePlatformHelper implements IPlatformHelper {
    private final NeoForgePlatformEvents events = new NeoForgePlatformEvents();

    @Override
    public String getPlatformName() {
        return "NeoForge";
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
