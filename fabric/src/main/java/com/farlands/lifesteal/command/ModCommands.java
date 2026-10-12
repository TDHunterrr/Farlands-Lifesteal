package com.farlands.lifesteal.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class ModCommands {

    public static void registerWithdrawCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LifestealCommands.registerWithdrawCommand(dispatcher);
    }

    public static void registerRemoveCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LifestealCommands.registerRemoveCommand(dispatcher);
    }

    public static void registerReviveCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LifestealCommands.registerReviveCommand(dispatcher);
    }
}
