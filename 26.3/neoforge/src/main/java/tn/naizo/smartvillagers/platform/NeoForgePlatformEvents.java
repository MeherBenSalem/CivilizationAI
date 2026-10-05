package tn.naizo.smartvillagers.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tn.naizo.smartvillagers.platform.ServerChatCallback;
import tn.naizo.smartvillagers.platform.CommandRegistrationCallback;

import java.util.function.Consumer;

public final class NeoForgePlatformEvents implements IPlatformEvents {
    @Override
    public void registerServerStarting(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerServerStopping(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerServerTick(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerPlayerJoin(Consumer<ServerPlayer> callback) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    @Override
    public void registerCommands(CommandRegistrationCallback callback) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                callback.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
    }

    @Override
    public void registerServerChat(ServerChatCallback callback) {
        NeoForge.EVENT_BUS.addListener((ServerChatEvent event) -> {
            if (callback.onChat(event.getPlayer(), event.getRawText())) {
                event.setCanceled(true);
            }
        });
    }
}
