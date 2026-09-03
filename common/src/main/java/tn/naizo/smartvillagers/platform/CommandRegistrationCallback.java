package tn.naizo.smartvillagers.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;

@FunctionalInterface
public interface CommandRegistrationCallback {
    void register(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher,
                  CommandBuildContext buildContext,
                  Commands.CommandSelection selection);
}
