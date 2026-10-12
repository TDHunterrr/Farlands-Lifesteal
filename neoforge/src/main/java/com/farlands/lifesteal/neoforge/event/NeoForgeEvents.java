package com.farlands.lifesteal.neoforge.event;

import com.farlands.lifesteal.LifestealConstants;
import com.farlands.lifesteal.command.LifestealCommands;
import com.farlands.lifesteal.item.ServerItems;
import com.farlands.lifesteal.state.LifestealState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@SuppressWarnings("null")
public class NeoForgeEvents {

    private final Map<UUID, Vec3> spawnProtectionPositions = new HashMap<>();
    private final Map<UUID, Integer> spawnProtectionTimes = new HashMap<>();

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Register Brigadier commands
        LifestealCommands.registerWithdrawCommand(event.getDispatcher());
        LifestealCommands.registerRemoveCommand(event.getDispatcher());
        LifestealCommands.registerReviveCommand(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        LifestealState state = LifestealState.getServerState(server);

        // Fix death loop: Give revived players hearts based on pending health, or default 20.0
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null && maxHealthAttr.getBaseValue() <= 0.0) {
            double targetHealth = state.hasPendingHealth(uuid) ? state.getPendingHealth(uuid) : LifestealConstants.DEFAULT_HEALTH;
            maxHealthAttr.setBaseValue(targetHealth);
            player.setHealth((float) targetHealth);

            if (state.hasPendingHealth(uuid)) {
                state.removePendingHealth(uuid);
            }
        }

        if (state.hasTeleport(uuid)) {
            BlockPos pos = state.getTeleport(uuid);
            player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            state.removeTeleport(uuid);
        }

        // Give 10 seconds of complete invulnerability (Resistance V) during loading terrain
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LifestealConstants.SPAWN_PROTECTION_TICKS, LifestealConstants.SPAWN_PROTECTION_AMPLIFIER, false, false, true));
        spawnProtectionPositions.put(uuid, player.position());
        spawnProtectionTimes.put(uuid, server.getTickCount());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (spawnProtectionPositions.isEmpty()) return;
        MinecraftServer server = event.getServer();
        int currentTick = server.getTickCount();

        Iterator<Map.Entry<UUID, Vec3>> iterator = spawnProtectionPositions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Vec3> entry = iterator.next();
            UUID uuid = entry.getKey();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);

            if (player == null || currentTick - spawnProtectionTimes.getOrDefault(uuid, currentTick) > LifestealConstants.SPAWN_PROTECTION_TICKS) {
                iterator.remove();
                spawnProtectionTimes.remove(uuid);
                if (player != null) {
                    player.removeEffect(MobEffects.RESISTANCE);
                }
            } else if (player.position().distanceToSqr(entry.getValue()) > 0.04) {
                // Cancel spawn protection immediately on movement
                iterator.remove();
                spawnProtectionTimes.remove(uuid);
                player.removeEffect(MobEffects.RESISTANCE);
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        double currentMax = maxHealthAttr.getBaseValue();
        double newMax = currentMax - LifestealConstants.HEALTH_PER_HEART;

        // Drop physical heart
        ItemStack droppedHeart = ServerItems.createHeart(1);
        ItemEntity heartEntity = player.drop(droppedHeart, true);
        if (heartEntity != null) {
            heartEntity.setPickUpDelay(40);
        }

        if (newMax <= 0.0) {
            maxHealthAttr.setBaseValue(0.0);
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                UserBanList banList = server.getPlayerList().getBans();
                NameAndId nameAndId = new NameAndId(player.getGameProfile());

                if (!banList.isBanned(nameAndId)) {
                    UserBanListEntry banEntry = new UserBanListEntry(
                            nameAndId,
                            new Date(),
                            "Lifesteal",
                            null,
                            "You ran out of hearts!"
                    );
                    banList.add(banEntry);

                    ItemStack reviveStack = ServerItems.createReviveToken(player.getName().getString(), player.getUUID());
                    ItemEntity itemEntity = player.drop(reviveStack, true);
                    if (itemEntity != null) {
                        itemEntity.setPickUpDelay(40);
                    }

                    if (player.connection != null) {
                        server.execute(() -> {
                            player.connection.disconnect(Component.literal("You lost all your hearts and have been banned!")
                                    .withStyle(ChatFormatting.RED));
                        });
                    }
                }
            }
        } else {
            maxHealthAttr.setBaseValue(newMax);
            player.sendSystemMessage(Component.literal("You lost a heart!").withStyle(ChatFormatting.RED));
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) || !(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;

        AttributeInstance oldAttr = oldPlayer.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance newAttr = newPlayer.getAttribute(Attributes.MAX_HEALTH);

        if (oldAttr != null && newAttr != null) {
            newAttr.setBaseValue(oldAttr.getBaseValue());
            if (newPlayer.getHealth() > newPlayer.getMaxHealth()) {
                newPlayer.setHealth(newPlayer.getMaxHealth());
            }
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItemStack();
        if (ServerItems.isHeart(stack)) {
            AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentMax = maxHealthAttr.getBaseValue();
                if (currentMax < LifestealConstants.MAX_HEALTH) {
                    maxHealthAttr.setBaseValue(currentMax + LifestealConstants.HEALTH_PER_HEART);
                    player.setHealth(player.getHealth() + (float) LifestealConstants.HEALTH_PER_HEART);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    player.sendSystemMessage(Component.literal("You gained 1 heart!").withStyle(ChatFormatting.GREEN));
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                } else {
                    player.sendSystemMessage(Component.literal("You already have the maximum amount of hearts!").withStyle(ChatFormatting.RED));
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItemStack();
        if (ServerItems.isHeart(stack)) {
            AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentMax = maxHealthAttr.getBaseValue();
                if (currentMax < LifestealConstants.MAX_HEALTH) {
                    maxHealthAttr.setBaseValue(currentMax + LifestealConstants.HEALTH_PER_HEART);
                    player.setHealth(player.getHealth() + (float) LifestealConstants.HEALTH_PER_HEART);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    player.sendSystemMessage(Component.literal("You gained 1 heart!").withStyle(ChatFormatting.GREEN));
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                } else {
                    player.sendSystemMessage(Component.literal("You already have the maximum amount of hearts!").withStyle(ChatFormatting.RED));
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                }
            }
            return;
        }

        if (ServerItems.isReviveToken(stack)) {
            BlockPos targetPos = event.getPos().above();
            MinecraftServer server = event.getLevel().getServer();
            if (server != null) {
                UUID bannedUUID = ServerItems.getTargetUUID(stack);
                String bannedName = ServerItems.getTargetName(stack);
                if (bannedUUID != null) {
                    UserBanList banList = server.getPlayerList().getBans();
                    NameAndId profileToUnban = null;

                    for (UserBanListEntry entry : banList.getEntries()) {
                        NameAndId user = entry.getUser();
                        if (user != null && user.id().equals(bannedUUID)) {
                            profileToUnban = user;
                            break;
                        }
                    }

                    if (profileToUnban != null) {
                        banList.remove(profileToUnban);
                        player.sendSystemMessage(Component.literal("Unbanned player: " + (bannedName != null ? bannedName : profileToUnban.name())).withStyle(ChatFormatting.GREEN));
                        LifestealState.addPendingTeleport(server, profileToUnban.id(), targetPos);
                        LifestealState.addPendingHealth(server, profileToUnban.id(), LifestealConstants.DEFAULT_HEALTH);
                        player.sendSystemMessage(Component.literal("They will be teleported to the targeted block upon joining.").withStyle(ChatFormatting.YELLOW));

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                    } else {
                        player.sendSystemMessage(Component.literal("This player is not banned!").withStyle(ChatFormatting.RED));
                        event.setCancellationResult(InteractionResult.FAIL);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}
