package com.farlands.lifesteal.event;

import com.farlands.lifesteal.LifestealConstants;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ModEvents {

    private static final Map<UUID, Vec3> spawnProtectionPositions = new HashMap<>();
    private static final Map<UUID, Integer> spawnProtectionTimes = new HashMap<>();

    public static void onPlayerMovePacket(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (spawnProtectionPositions.containsKey(uuid)) {
            spawnProtectionPositions.remove(uuid);
            spawnProtectionTimes.remove(uuid);
            player.removeEffect(MobEffects.RESISTANCE);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        UUID uuid = player.getUUID();

        LifestealState state = LifestealState.getServerState(server);

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

        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LifestealConstants.SPAWN_PROTECTION_TICKS, LifestealConstants.SPAWN_PROTECTION_AMPLIFIER, false, false, true));
        spawnProtectionPositions.put(uuid, player.position());
        spawnProtectionTimes.put(uuid, server.getTickCount());
    }

    public static void onServerTick(MinecraftServer server) {
        if (spawnProtectionPositions.isEmpty()) return;

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
            }
        }
    }

    public static InteractionResult onUseItem(ServerPlayer player, net.minecraft.world.level.Level level, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);

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
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendSystemMessage(Component.literal("You already have the maximum amount of hearts!").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.PASS;
    }

    public static InteractionResult onUseBlock(ServerPlayer player, net.minecraft.world.level.Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);

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
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendSystemMessage(Component.literal("You already have the maximum amount of hearts!").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.FAIL;
        }

        if (ServerItems.isReviveToken(stack)) {
            BlockPos targetPos = hitResult.getBlockPos().above();
            MinecraftServer server = level.getServer();
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
                        return InteractionResult.SUCCESS;
                    } else {
                        player.sendSystemMessage(Component.literal("This player is not banned!").withStyle(ChatFormatting.RED));
                        return InteractionResult.FAIL;
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }
}
