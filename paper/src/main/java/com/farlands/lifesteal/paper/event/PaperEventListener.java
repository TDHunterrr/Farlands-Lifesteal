package com.farlands.lifesteal.paper.event;

import com.farlands.lifesteal.LifestealConstants;
import com.farlands.lifesteal.paper.FarlandsLifestealPlugin;
import com.farlands.lifesteal.paper.item.PaperItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PaperEventListener implements Listener {

    private final FarlandsLifestealPlugin plugin;
    private final Map<UUID, Location> spawnProtectionPositions = new HashMap<>();

    public PaperEventListener(FarlandsLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    private Attribute getMaxHealthAttribute() {
        return Attribute.MAX_HEALTH;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        AttributeInstance maxHealthAttr = player.getAttribute(getMaxHealthAttribute());
        if (maxHealthAttr != null && (maxHealthAttr.getBaseValue() <= 0.0 || plugin.hasPendingHealth(uuid))) {
            double targetHealth = plugin.hasPendingHealth(uuid) ? plugin.getPendingHealth(uuid) : LifestealConstants.DEFAULT_HEALTH;
            maxHealthAttr.setBaseValue(targetHealth);
            player.setHealth(targetHealth);

            if (plugin.hasPendingHealth(uuid)) {
                plugin.removePendingHealth(uuid);
            }
        }

        if (plugin.hasPendingTeleport(uuid)) {
            Location loc = plugin.getPendingTeleport(uuid);
            plugin.runTaskLater(player, () -> player.teleport(loc), 1L);
            plugin.removePendingTeleport(uuid);
        }

        // 10s Resistance V spawn protection during loading terrain
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, LifestealConstants.SPAWN_PROTECTION_TICKS, LifestealConstants.SPAWN_PROTECTION_AMPLIFIER, false, false, true));
        spawnProtectionPositions.put(uuid, player.getLocation());

        plugin.runTaskLater(player, () -> {
            if (spawnProtectionPositions.containsKey(uuid)) {
                spawnProtectionPositions.remove(uuid);
                player.removePotionEffect(PotionEffectType.RESISTANCE);
            }
        }, LifestealConstants.SPAWN_PROTECTION_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (spawnProtectionPositions.containsKey(uuid)) {
            Location from = spawnProtectionPositions.get(uuid);
            Location to = event.getTo();
            if (to != null && from.getWorld().equals(to.getWorld()) && from.distanceSquared(to) > 0.04) {
                // Cancel spawn protection immediately on movement
                spawnProtectionPositions.remove(uuid);
                player.removePotionEffect(PotionEffectType.RESISTANCE);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        // Using Heart
        if (PaperItems.isHeart(plugin, item) && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            AttributeInstance maxHealthAttr = player.getAttribute(getMaxHealthAttribute());
            if (maxHealthAttr != null) {
                double currentMax = maxHealthAttr.getBaseValue();
                if (currentMax < LifestealConstants.MAX_HEALTH) {
                    maxHealthAttr.setBaseValue(currentMax + LifestealConstants.HEALTH_PER_HEART);
                    player.setHealth(Math.min(player.getHealth() + LifestealConstants.HEALTH_PER_HEART, maxHealthAttr.getBaseValue()));
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        item.subtract(1);
                    }
                    player.sendMessage(Component.text("You gained 1 heart!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("You already have the maximum amount of hearts!", NamedTextColor.RED));
                }
            }
            return;
        }

        // Using Revive Token on block
        if (PaperItems.isReviveToken(plugin, item) && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            event.setCancelled(true);
            UUID targetUuid = PaperItems.getTargetUUID(plugin, item);
            String targetName = PaperItems.getTargetName(plugin, item);

            if (targetUuid != null && targetName != null) {
                BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                if (banList.isBanned(targetName)) {
                    banList.pardon(targetName);

                    Block targetBlock = event.getClickedBlock().getRelative(BlockFace.UP);
                    Location teleportLoc = targetBlock.getLocation().add(0.5, 0.0, 0.5);

                    plugin.addPendingTeleport(targetUuid, teleportLoc);
                    plugin.addPendingHealth(targetUuid, LifestealConstants.DEFAULT_HEALTH);

                    player.sendMessage(Component.text("Unbanned player: " + targetName, NamedTextColor.GREEN));
                    player.sendMessage(Component.text("They will be teleported to the targeted block upon joining.", NamedTextColor.YELLOW));

                    if (player.getGameMode() != GameMode.CREATIVE) {
                        item.subtract(1);
                    }
                } else {
                    player.sendMessage(Component.text("This player is not banned!", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        AttributeInstance maxHealthAttr = player.getAttribute(getMaxHealthAttribute());
        if (maxHealthAttr == null) return;

        double currentMax = maxHealthAttr.getBaseValue();
        double newMax = currentMax - LifestealConstants.HEALTH_PER_HEART;

        // Drop physical heart
        player.getWorld().dropItemNaturally(player.getLocation(), PaperItems.createHeart(plugin, 1));

        if (newMax <= 0.0) {
            maxHealthAttr.setBaseValue(0.0);
            String playerName = player.getName();
            UUID playerUuid = player.getUniqueId();

            Bukkit.getBanList(BanList.Type.NAME).addBan(playerName, "You ran out of hearts!", null, "Lifesteal");

            // Drop Revive Token
            player.getWorld().dropItemNaturally(player.getLocation(), PaperItems.createReviveToken(plugin, playerName, playerUuid));

            // Disconnect player
            plugin.runTask(player, () -> player.kick(Component.text("You lost all your hearts and have been banned!", NamedTextColor.RED)));
        } else {
            maxHealthAttr.setBaseValue(newMax);
            player.sendMessage(Component.text("You lost a heart!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        AttributeInstance maxHealthAttr = player.getAttribute(getMaxHealthAttribute());
        if (maxHealthAttr != null && maxHealthAttr.getBaseValue() > 0.0) {
            player.setHealth(maxHealthAttr.getBaseValue());
        }
    }
}
