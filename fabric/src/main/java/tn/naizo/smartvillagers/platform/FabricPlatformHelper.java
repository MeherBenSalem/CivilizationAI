package tn.naizo.smartvillagers.platform;

import net.fabricmc.loader.api.FabricLoader;
import tn.naizo.smartvillagers.platform.IPlatformHelper;

import java.nio.file.Path;

public final class FabricPlatformHelper implements IPlatformHelper {
    private final FabricPlatformEvents events = new FabricPlatformEvents();

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public IPlatformEvents events() {
        return events;
    }
}
