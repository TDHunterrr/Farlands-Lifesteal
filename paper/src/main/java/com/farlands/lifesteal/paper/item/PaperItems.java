package com.farlands.lifesteal.paper.item;

import com.farlands.lifesteal.LifestealConstants;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaperItems {

    private static NamespacedKey itemKey(Plugin plugin) {
        return new NamespacedKey(plugin, LifestealConstants.TAG_LIFESTEAL_ITEM);
    }

    private static NamespacedKey targetUuidKey(Plugin plugin) {
        return new NamespacedKey(plugin, LifestealConstants.TAG_TARGET_UUID);
    }

    private static NamespacedKey targetNameKey(Plugin plugin) {
        return new NamespacedKey(plugin, LifestealConstants.TAG_TARGET_NAME);
    }

    public static ItemStack createHeart(Plugin plugin, int amount) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, amount);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Heart", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Right-click to gain 1 heart.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("The essence of life.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(itemKey(plugin), PersistentDataType.STRING, LifestealConstants.VALUE_HEART);

            try {
                PlayerProfile profile = Bukkit.createProfile(LifestealConstants.HEART_SKULL_UUID, LifestealConstants.HEART_SKULL_NAME);
                profile.setProperty(new ProfileProperty("textures", LifestealConstants.HEART_TEXTURE_BASE64));
                meta.setPlayerProfile(profile);
            } catch (Exception ignored) {}

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createReviveToken(Plugin plugin, String playerName, UUID playerUuid) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Revive Token - " + playerName, NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Right-click a block to unban the player.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("They will be teleported to the block you look at.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Revives player: " + playerName, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(itemKey(plugin), PersistentDataType.STRING, LifestealConstants.VALUE_REVIVE_TOKEN);
            pdc.set(targetUuidKey(plugin), PersistentDataType.STRING, playerUuid.toString());
            pdc.set(targetNameKey(plugin), PersistentDataType.STRING, playerName);

            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUuid));
            } catch (Exception ignored) {}

            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isHeart(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return false;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return false;
        String val = meta.getPersistentDataContainer().get(itemKey(plugin), PersistentDataType.STRING);
        return LifestealConstants.VALUE_HEART.equals(val);
    }

    public static boolean isReviveToken(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return false;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return false;
        String val = meta.getPersistentDataContainer().get(itemKey(plugin), PersistentDataType.STRING);
        return LifestealConstants.VALUE_REVIVE_TOKEN.equals(val);
    }

    public static UUID getTargetUUID(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return null;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(targetUuidKey(plugin), PersistentDataType.STRING);
        if (raw != null) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public static String getTargetName(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return null;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(targetNameKey(plugin), PersistentDataType.STRING);
    }
}
