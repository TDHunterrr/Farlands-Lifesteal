package com.farlands.lifesteal.mixin;


import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Date;

@Mixin(ServerPlayer.class)
@SuppressWarnings("null")
public abstract class ServerPlayerEntityMixin {

    @Shadow
    public MinecraftServer server;

    @Shadow
    public net.minecraft.server.network.ServerGamePacketListenerImpl connection;

    @Inject(method = "die", at = @At("TAIL"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealthAttr != null) {
            double currentMax = maxHealthAttr.getBaseValue();
            double newMax = currentMax - 2.0;

            ItemStack droppedHeart = com.farlands.lifesteal.item.ServerItems.createHeart(1);
            ItemEntity heartEntity = player.drop(droppedHeart, true);
            if (heartEntity != null) {
                heartEntity.setPickUpDelay(40);
            }

            if (newMax <= 0.0) {
                maxHealthAttr.setBaseValue(0.0);
                MinecraftServer mcServer = this.server;
                if (mcServer != null) {
                    UserBanList banList = mcServer.getPlayerList().getBans();
                    NameAndId nameAndId = new NameAndId(player.getGameProfile());

                    if (!banList.isBanned(nameAndId)) {
                        UserBanListEntry entry = new UserBanListEntry(
                                nameAndId,
                                new Date(),
                                "Lifesteal",
                                null,
                                "You ran out of hearts!");
                        banList.add(entry);

                        ItemStack reviveStack = com.farlands.lifesteal.item.ServerItems.createReviveToken(player.getName().getString(), player.getUUID());
                        ItemEntity itemEntity = player.drop(reviveStack, true);
                        if (itemEntity != null) {
                            itemEntity.setPickUpDelay(40);
                        }

                        if (this.connection != null) {
                            mcServer.execute(() -> {
                                this.connection.disconnect(Component.literal("You lost all your hearts and have been banned!")
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
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        AttributeInstance oldMaxHealthAttr = oldPlayer.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance newMaxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);

        if (oldMaxHealthAttr != null && newMaxHealthAttr != null) {
            newMaxHealthAttr.setBaseValue(oldMaxHealthAttr.getBaseValue());
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}
