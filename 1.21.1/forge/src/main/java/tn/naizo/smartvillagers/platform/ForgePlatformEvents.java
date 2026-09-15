package tn.naizo.smartvillagers.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import tn.naizo.smartvillagers.platform.ServerChatCallback;
import tn.naizo.smartvillagers.platform.CommandRegistrationCallback;

import java.util.function.Consumer;

public final class ForgePlatformEvents implements IPlatformEvents {
    @Override
    public void registerServerStarting(Consumer<MinecraftServer> callback) {
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerServerStopping(Consumer<MinecraftServer> callback) {
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerServerTick(Consumer<MinecraftServer> callback) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                callback.accept(event.getServer());
            }
        });
    }

    @Override
    public void registerPlayerJoin(Consumer<ServerPlayer> callback) {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    @Override
    public void registerCommands(CommandRegistrationCallback callback) {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                callback.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
    }

    @Override
    public void registerServerChat(ServerChatCallback callback) {
        MinecraftForge.EVENT_BUS.addListener((ServerChatEvent event) -> {
            if (callback.onChat(event.getPlayer(), event.getRawText())) {
                event.setCanceled(true);
            }
        });
    }
}
