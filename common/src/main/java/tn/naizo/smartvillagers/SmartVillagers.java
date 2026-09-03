package tn.naizo.smartvillagers;

import tn.naizo.smartvillagers.chat.ConversationService;
import tn.naizo.smartvillagers.chat.ProximityChatHandler;
import tn.naizo.smartvillagers.command.VillagerAiCommands;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.platform.Services;

public final class SmartVillagers {
    private static final Object LOCK = new Object();
    private static volatile boolean initialized;
    private static ConversationService conversations;
    private static ProximityChatHandler chatHandler;

    private SmartVillagers() {
    }

    public static void initialize() {
        synchronized (LOCK) {
            if (initialized) {
                return;
            }

            Constants.LOG.info("Initializing {} on {}", Constants.MOD_NAME, Services.PLATFORM.getPlatformName());
            SmartVillagersConfig.load();

            conversations = new ConversationService();
            conversations.boot();
            chatHandler = new ProximityChatHandler(conversations);

            var events = Services.PLATFORM.events();
            events.registerServerChat((player, message) -> chatHandler.onChat(player, message));
            events.registerCommands((dispatcher, buildContext, selection) ->
                    VillagerAiCommands.register(dispatcher, conversations));
            events.registerServerTick(conversations::tick);
            events.registerServerStopping(server -> conversations.shutdown());
            events.registerPlayerJoin(player -> conversations.privacy().sendJoinNotice(player));

            initialized = true;
            Constants.LOG.info("{} ready", Constants.MOD_NAME);
        }
    }

    public static ConversationService conversations() {
        return conversations;
    }
}
