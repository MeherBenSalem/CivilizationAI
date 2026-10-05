package tn.naizo.smartvillagers.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public interface IPlatformEvents {
    void registerServerStarting(Consumer<MinecraftServer> callback);

    void registerServerStopping(Consumer<MinecraftServer> callback);

    void registerServerTick(Consumer<MinecraftServer> callback);

    void registerPlayerJoin(Consumer<ServerPlayer> callback);

    void registerCommands(CommandRegistrationCallback callback);

    void registerServerChat(ServerChatCallback callback);
}
