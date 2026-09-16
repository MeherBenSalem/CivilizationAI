package tn.naizo.smartvillagers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import tn.naizo.smartvillagers.chat.ConversationService;
import tn.naizo.smartvillagers.config.ApiCredentials;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;
import tn.naizo.smartvillagers.platform.Services;
import tn.naizo.smartvillagers.villager.PersonaOverride;
import tn.naizo.smartvillagers.voice.VoiceAvailability;
import tn.naizo.smartvillagers.voice.VoiceOutput;
import tn.naizo.smartvillagers.villager.VillagerAiData;
import tn.naizo.smartvillagers.villager.VillagerAiDataHolder;
import tn.naizo.smartvillagers.villager.VillagerIdentity;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.Comparator;
import java.util.Optional;

public final class VillagerAiCommands {
    private VillagerAiCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ConversationService conversations) {
        dispatcher.register(Commands.literal("villagerai")
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("status")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> status(ctx.getSource(), conversations)))
                .then(Commands.literal("debug")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> debug(ctx.getSource(), conversations)))
                .then(Commands.literal("test")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> testNearest(ctx.getSource(), conversations))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> testTarget(ctx.getSource(), conversations,
                                        EntityArgument.getEntity(ctx, "target")))))
                .then(Commands.literal("memory")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearLookedMemory(ctx.getSource()))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> clearTargetMemory(ctx.getSource(),
                                                EntityArgument.getEntity(ctx, "target"))))))
                .then(Commands.literal("consent")
                        .then(Commands.literal("accept")
                                .executes(ctx -> setConsent(ctx.getSource(), conversations, true)))
                        .then(Commands.literal("decline")
                                .executes(ctx -> setConsent(ctx.getSource(), conversations, false))))
                .then(Commands.literal("persona")
                        .requires(VillagerAiCommands::canEditPersona)
                        .then(Commands.literal("get")
                                .executes(ctx -> personaGet(ctx.getSource(), null))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> personaGet(ctx.getSource(),
                                                EntityArgument.getEntity(ctx, "target")))))
                        .then(Commands.literal("set")
                                .then(Commands.literal("name")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> personaSet(ctx, "name"))))
                                .then(Commands.literal("trait")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> personaSet(ctx, "trait"))))
                                .then(Commands.literal("style")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> personaSet(ctx, "style"))))
                                .then(Commands.literal("backstory")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> personaSet(ctx, "backstory")))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> personaClear(ctx.getSource())))));
    }

    private static boolean canEditPersona(CommandSourceStack source) {
        if (SmartVillagersConfig.get().allowPlayersEditPersona()) {
            return true;
        }
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
    }

    private static int reload(CommandSourceStack source) {
        SmartVillagersConfig.refresh();
        ApiCredentials.reload();
        source.sendSuccess(() -> Component.literal("Smart Villagers AI configuration reloaded.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int status(CommandSourceStack source, ConversationService conversations) {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        source.sendSuccess(() -> Component.literal("Smart Villagers AI").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        line(source, "Proximity", config.proximityEnabled() ? "enabled" : "disabled");
        line(source, "Activation", config.activationMode().name());
        line(source, "Hearing / response", config.hearingRadius() + " / " + config.responseRadius());
        line(source, "Display", config.displayMode().name());
        line(source, "Provider", conversations.provider().name()
                + (conversations.provider().isConfigured() ? " (ready)" : " (not configured)"));
        line(source, "API key source", ApiCredentials.source());
        line(source, "Opt-in required", String.valueOf(config.requirePlayerOptIn()));
        boolean svc = VoiceAvailability.isModLoaded(id -> {
            try {
                return Services.PLATFORM.isModLoaded(id);
            } catch (Throwable t) {
                return false;
            }
        });
        line(source, "Voice", config.voiceEnabled() ? "enabled" : "disabled");
        line(source, "Simple Voice Chat", svc ? "present" : "missing (text only)");
        line(source, "Voice backend", VoiceOutput.backendReady() ? "ready" : "not ready");
        line(source, "Voice volume / range", config.voiceVolume() + " / "
                + (config.voiceRange() > 0 ? config.voiceRange() : config.responseRadius() + " (responseRadius)"));
        return 1;
    }

    private static int testNearest(CommandSourceStack source, ConversationService conversations) {
        return resolveVillager(source, null, 16.0).map(villager ->
                runTest(source, conversations, villager)).orElseGet(() -> {
            source.sendFailure(Component.literal("Stand next to a villager (within 16 blocks) or provide a target."));
            return 0;
        });
    }

    private static int testTarget(CommandSourceStack source, ConversationService conversations, Entity target) {
        if (!(target instanceof Villager villager)) {
            source.sendFailure(Component.literal("Target is not a villager."));
            return 0;
        }
        return runTest(source, conversations, villager);
    }

    private static int runTest(CommandSourceStack source, ConversationService conversations, Villager villager) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }

        VillagerPersona persona = VillagerIdentity.applyVisibleIdentity(villager);
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        source.sendSuccess(() -> Component.literal("Smart Villagers AI test")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        line(source, "Identity", persona.displayName() + " / " + persona.trait() + " / " + persona.professionLabel());
        line(source, "Mixin", villager instanceof VillagerAiDataHolder ? "ok" : "FAILED (villager has no AI data)");
        line(source, "Provider", conversations.provider().name()
                + (conversations.provider().isConfigured() ? " (ready)" : " (not configured)"));
        line(source, "API key source", ApiCredentials.source());
        line(source, "Model", config.model());
        line(source, "API URL", config.apiBaseUrl());
        line(source, "Consent", conversations.privacy().hasConsent(player) ? "yes" : "no (test still calls AI)");
        boolean svc = VoiceAvailability.isModLoaded(id -> {
            try {
                return Services.PLATFORM.isModLoaded(id);
            } catch (Throwable t) {
                return false;
            }
        });
        line(source, "Voice", config.voiceEnabled() ? "enabled" : "disabled");
        line(source, "Simple Voice Chat", svc ? "present" : "missing (text only)");
        line(source, "Voice backend", VoiceOutput.backendReady() ? "ready" : "not ready");
        source.sendSuccess(() -> Component.literal("Sending test line to " + persona.displayName()
                        + " — wait up to 30s for DeepSeek, then a chat line and Voice spoken: yes/no.")
                .withStyle(ChatFormatting.YELLOW), false);

        boolean started = conversations.testTalk(player, villager);
        return started ? 1 : 0;
    }

    private static int debug(CommandSourceStack source, ConversationService conversations) {
        source.sendSuccess(() -> Component.literal("Smart Villagers AI - live state")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        line(source, "Sessions", String.valueOf(conversations.sessions().session(source.getEntity() != null
                ? source.getEntity().getUUID() : java.util.UUID.randomUUID()).isPresent()));
        line(source, "Pending replies", String.valueOf(conversations.pendingResponses().size()));
        line(source, "Rate limiter concurrent", String.valueOf(conversations.rateLimiter().inFlight()));
        return 1;
    }

    private static int clearLookedMemory(CommandSourceStack source) {
        return resolveVillager(source, null).map(villager -> {
            VillagerAiData.get(villager).memory().clearAll();
            source.sendSuccess(() -> Component.literal("Cleared memory for " + VillagerPersona.from(villager).displayName()), true);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.literal("Look at a villager (within 6 blocks) or provide a target."));
            return 0;
        });
    }

    private static int clearTargetMemory(CommandSourceStack source, Entity target) {
        if (!(target instanceof Villager villager)) {
            source.sendFailure(Component.literal("Target is not a villager."));
            return 0;
        }
        VillagerAiData.get(villager).memory().clearAll();
        source.sendSuccess(() -> Component.literal("Cleared memory for " + VillagerPersona.from(villager).displayName()), true);
        return 1;
    }

    private static int setConsent(CommandSourceStack source, ConversationService conversations, boolean accepted) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }
        conversations.privacy().setConsent(player, accepted);
        source.sendSuccess(() -> Component.literal(accepted
                        ? "AI consent accepted. Nearby villagers may use the AI provider."
                        : "AI consent declined. Local fallback replies only.")
                .withStyle(accepted ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int personaGet(CommandSourceStack source, Entity target) {
        return resolveVillager(source, target).map(villager -> {
            VillagerPersona persona = VillagerPersona.from(villager);
            PersonaOverride override = VillagerAiData.get(villager).personaOverride();
            source.sendSuccess(() -> Component.literal("Persona for " + persona.displayName())
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
            line(source, "Trait", persona.trait());
            line(source, "Style", persona.speechStyle());
            line(source, "Backstory", persona.backstory());
            line(source, "Profession", persona.professionLabel());
            line(source, "Overrides", override.isEmpty() ? "none" : override.toString());
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.literal("Look at a villager (within 6 blocks) or provide a target."));
            return 0;
        });
    }

    private static int personaSet(CommandContext<CommandSourceStack> ctx, String field) {
        CommandSourceStack source = ctx.getSource();
        String value = StringArgumentType.getString(ctx, "value");
        return resolveVillager(source, null).map(villager -> {
            VillagerAiData data = VillagerAiData.get(villager);
            PersonaOverride current = data.personaOverride();
            PersonaOverride updated = switch (field) {
                case "name" -> current.withName(value);
                case "trait" -> current.withTrait(value);
                case "style" -> current.withSpeechStyle(value);
                case "backstory" -> current.withBackstory(value);
                default -> current;
            };
            data.setPersonaOverride(updated);
            if ("name".equals(field)) {
                villager.setCustomName(Component.literal(value));
                villager.setCustomNameVisible(true);
            }
            source.sendSuccess(() -> Component.literal("Updated persona " + field + " for "
                    + VillagerPersona.from(villager).displayName()).withStyle(ChatFormatting.GREEN), true);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.literal("Look at a villager (within 6 blocks)."));
            return 0;
        });
    }

    private static int personaClear(CommandSourceStack source) {
        return resolveVillager(source, null).map(villager -> {
            VillagerAiData.get(villager).setPersonaOverride(PersonaOverride.EMPTY);
            source.sendSuccess(() -> Component.literal("Cleared persona overrides for "
                    + VillagerPersona.from(villager).displayName()).withStyle(ChatFormatting.GREEN), true);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.literal("Look at a villager (within 6 blocks)."));
            return 0;
        });
    }

    private static Optional<Villager> resolveVillager(CommandSourceStack source, Entity explicit) {
        return resolveVillager(source, explicit, 6.0);
    }

    private static Optional<Villager> resolveVillager(CommandSourceStack source, Entity explicit, double range) {
        if (explicit instanceof Villager villager) {
            return Optional.of(villager);
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return Optional.empty();
        }
        AABB box = player.getBoundingBox().inflate(range);
        return player.level().getEntitiesOfClass(Villager.class, box, Villager::isAlive).stream()
                .min(Comparator.comparingDouble(player::distanceTo));
    }

    private static void line(CommandSourceStack source, String label, String value) {
        source.sendSuccess(() -> Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE)), false);
    }
}
