package com.farlands.lifesteal.command;

import com.farlands.lifesteal.LifestealConstants;
import com.farlands.lifesteal.item.ServerItems;
import com.farlands.lifesteal.state.LifestealState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

@SuppressWarnings("null")
public class LifestealCommands {

    public static void registerWithdrawCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("withdrawheart")
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);

                            if (maxHealthAttr != null) {
                                double currentMax = maxHealthAttr.getBaseValue();
                                double healthToLose = amount * LifestealConstants.HEALTH_PER_HEART;

                                if (currentMax - healthToLose > 0) {
                                    maxHealthAttr.setBaseValue(currentMax - healthToLose);
                                    if (player.getHealth() > currentMax - healthToLose) {
                                        player.setHealth((float) (currentMax - healthToLose));
                                    }

                                    ItemStack hearts = ServerItems.createHeart(amount);
                                    if (!player.getInventory().add(hearts)) {
                                        player.drop(hearts, false);
                                    }

                                    context.getSource().sendSuccess(() -> Component.literal("Withdrew " + amount + " heart(s).").withStyle(ChatFormatting.GREEN), false);
                                    return 1;
                                } else {
                                    context.getSource().sendFailure(Component.literal("You don't have enough hearts to withdraw that many!"));
                                    return 0;
                                }
                            }
                            return 0;
                        })
                )
        );
    }

    public static void registerRemoveCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("removeheart")
                .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");

                                    AttributeInstance maxHealthAttr = target.getAttribute(Attributes.MAX_HEALTH);
                                    if (maxHealthAttr != null) {
                                        double currentMax = maxHealthAttr.getBaseValue();
                                        double healthToLose = amount * LifestealConstants.HEALTH_PER_HEART;
                                        double newMax = Math.max(LifestealConstants.MIN_HEALTH, currentMax - healthToLose);

                                        maxHealthAttr.setBaseValue(newMax);
                                        if (target.getHealth() > newMax) {
                                            target.setHealth((float) newMax);
                                        }

                                        context.getSource().sendSuccess(() -> Component.literal("Removed " + amount + " heart(s) from " + target.getName().getString()).withStyle(ChatFormatting.GREEN), true);

                                        if (newMax <= 0.0) {
                                            target.kill((ServerLevel) target.level());
                                        }
                                        return 1;
                                    }
                                    return 0;
                                })
                        )
                )
        );
    }

    public static void registerReviveCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("revive")
                .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(context -> revivePlayer(context, LifestealConstants.DEFAULT_HEARTS))
                        .then(Commands.argument("hearts", IntegerArgumentType.integer(1, LifestealConstants.MAX_HEARTS))
                                .executes(context -> revivePlayer(context, IntegerArgumentType.getInteger(context, "hearts")))
                        )
                )
        );
    }

    private static int revivePlayer(CommandContext<CommandSourceStack> context, int hearts) throws CommandSyntaxException {
        Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, "player");
        if (profiles.isEmpty()) return 0;

        NameAndId profile = profiles.iterator().next();
        MinecraftServer server = context.getSource().getServer();
        UserBanList banList = server.getPlayerList().getBans();

        boolean found = false;
        for (UserBanListEntry entry : banList.getEntries()) {
            NameAndId user = entry.getUser();
            if (user != null && user.id().equals(profile.id())) {
                banList.remove(user);
                found = true;
                break;
            }
        }

        if (found) {
            LifestealState.addPendingHealth(server, profile.id(), hearts * LifestealConstants.HEALTH_PER_HEART);
            context.getSource().sendSuccess(() -> Component.literal("Revived " + profile.name() + " with " + hearts + " hearts!").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal(profile.name() + " is not banned/dead!"));
            return 0;
        }
    }
}
