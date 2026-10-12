package com.farlands.lifesteal.item;

import com.farlands.lifesteal.LifestealConstants;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("null")
public class ServerItems {

    public static ItemStack createHeart(int amount) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD, amount);

        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Heart").withStyle(style -> style.withColor(ChatFormatting.RED).withBold(true).withItalic(false)));

        List<Component> heartLore = new ArrayList<>();
        heartLore.add(Component.literal("Right-click to gain 1 heart.").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withItalic(false)));
        heartLore.add(Component.literal("The essence of life.").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(heartLore));

        CompoundTag tag = new CompoundTag();
        tag.putString(LifestealConstants.TAG_LIFESTEAL_ITEM, LifestealConstants.VALUE_HEART);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();
        builder.put("textures", new Property("textures", LifestealConstants.HEART_TEXTURE_BASE64));
        PropertyMap properties = new PropertyMap(builder.build());
        GameProfile profile = new GameProfile(LifestealConstants.HEART_SKULL_UUID, LifestealConstants.HEART_SKULL_NAME, properties);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));

        return stack;
    }

    public static ItemStack createReviveToken(String playerName, UUID playerUuid) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD, 1);

        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Revive Token - " + playerName).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

        List<Component> tokenLore = new ArrayList<>();
        tokenLore.add(Component.literal("Right-click a block to unban the player.").withStyle(ChatFormatting.RED));
        tokenLore.add(Component.literal("They will be teleported to the block you look at.").withStyle(ChatFormatting.GRAY));
        tokenLore.add(Component.literal("Revives player: " + playerName).withStyle(ChatFormatting.AQUA));
        stack.set(DataComponents.LORE, new ItemLore(tokenLore));

        CompoundTag tag = new CompoundTag();
        tag.putString(LifestealConstants.TAG_TARGET_UUID, playerUuid.toString());
        tag.putString(LifestealConstants.TAG_TARGET_NAME, playerName);
        tag.putString(LifestealConstants.TAG_LIFESTEAL_ITEM, LifestealConstants.VALUE_REVIVE_TOKEN);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(playerName));

        return stack;
    }

    public static boolean isHeart(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) return false;
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData.copyTag();
            if (tag.contains(LifestealConstants.TAG_LIFESTEAL_ITEM)) {
                net.minecraft.nbt.Tag itemTag = tag.get(LifestealConstants.TAG_LIFESTEAL_ITEM);
                if (itemTag != null) {
                    return itemTag.toString().replace("\"", "").equals(LifestealConstants.VALUE_HEART);
                }
            }
        }
        return false;
    }

    public static boolean isReviveToken(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) return false;
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData.copyTag();
            if (tag.contains(LifestealConstants.TAG_LIFESTEAL_ITEM)) {
                net.minecraft.nbt.Tag itemTag = tag.get(LifestealConstants.TAG_LIFESTEAL_ITEM);
                if (itemTag != null) {
                    return itemTag.toString().replace("\"", "").equals(LifestealConstants.VALUE_REVIVE_TOKEN);
                }
            }
        }
        return false;
    }

    public static UUID getTargetUUID(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData.copyTag();
            if (tag.contains(LifestealConstants.TAG_TARGET_UUID)) {
                net.minecraft.nbt.Tag uuidTag = tag.get(LifestealConstants.TAG_TARGET_UUID);
                if (uuidTag != null) {
                    try {
                        return UUID.fromString(uuidTag.toString().replace("\"", ""));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return null;
    }

    public static String getTargetName(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData.copyTag();
            if (tag.contains(LifestealConstants.TAG_TARGET_NAME)) {
                net.minecraft.nbt.Tag nameTag = tag.get(LifestealConstants.TAG_TARGET_NAME);
                if (nameTag != null) {
                    return nameTag.toString().replace("\"", "");
                }
            }
        }
        return null;
    }
}
