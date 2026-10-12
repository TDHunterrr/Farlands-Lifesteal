package com.farlands.lifesteal.paper.command;

import com.farlands.lifesteal.LifestealConstants;
import com.farlands.lifesteal.paper.FarlandsLifestealPlugin;
import com.farlands.lifesteal.paper.item.PaperItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class PaperCommands implements CommandExecutor {

    private final FarlandsLifestealPlugin plugin;

    public PaperCommands(FarlandsLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    private Attribute getMaxHealthAttribute() {
        return Attribute.MAX_HEALTH;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("withdrawheart")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(Component.text("Usage: /withdrawheart <amount>", NamedTextColor.RED));
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[0]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Amount must be a positive integer!", NamedTextColor.RED));
                return true;
            }

            AttributeInstance attr = player.getAttribute(getMaxHealthAttribute());
            if (attr != null) {
                double currentMax = attr.getBaseValue();
                double healthToLose = amount * LifestealConstants.HEALTH_PER_HEART;

                if (currentMax - healthToLose > 0) {
                    attr.setBaseValue(currentMax - healthToLose);
                    if (player.getHealth() > attr.getBaseValue()) {
                        player.setHealth(attr.getBaseValue());
                    }

                    ItemStack hearts = PaperItems.createHeart(plugin, amount);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(hearts);
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }

                    sender.sendMessage(Component.text("Withdrew " + amount + " heart(s).", NamedTextColor.GREEN));
                    return true;
                } else {
                    sender.sendMessage(Component.text("You don't have enough hearts to withdraw that many!", NamedTextColor.RED));
                    return true;
                }
            }
            return true;
        }

        if (cmd.equals("removeheart")) {
            if (!sender.hasPermission("farlands.lifesteal.admin")) {
                sender.sendMessage(Component.text("You do not have permission to execute this command.", NamedTextColor.RED));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /removeheart <player> <amount>", NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found or not online!", NamedTextColor.RED));
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Amount must be a positive integer!", NamedTextColor.RED));
                return true;
            }

            AttributeInstance attr = target.getAttribute(getMaxHealthAttribute());
            if (attr != null) {
                double currentMax = attr.getBaseValue();
                double healthToLose = amount * LifestealConstants.HEALTH_PER_HEART;
                double newMax = Math.max(LifestealConstants.MIN_HEALTH, currentMax - healthToLose);

                attr.setBaseValue(newMax);
                if (target.getHealth() > newMax) {
                    target.setHealth(newMax);
                }

                sender.sendMessage(Component.text("Removed " + amount + " heart(s) from " + target.getName(), NamedTextColor.GREEN));

                if (newMax <= 0.0) {
                    target.setHealth(0.0);
                }
                return true;
            }
            return true;
        }

        if (cmd.equals("revive")) {
            if (!sender.hasPermission("farlands.lifesteal.admin")) {
                sender.sendMessage(Component.text("You do not have permission to execute this command.", NamedTextColor.RED));
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(Component.text("Usage: /revive <player> [hearts]", NamedTextColor.RED));
                return true;
            }

            String targetName = args[0];
            int hearts = LifestealConstants.DEFAULT_HEARTS;
            if (args.length >= 2) {
                try {
                    hearts = Math.min(LifestealConstants.MAX_HEARTS, Math.max(1, Integer.parseInt(args[1])));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Hearts must be an integer between 1 and " + LifestealConstants.MAX_HEARTS + "!", NamedTextColor.RED));
                    return true;
                }
            }

            BanList banList = Bukkit.getBanList(BanList.Type.NAME);
            if (banList.isBanned(targetName)) {
                banList.pardon(targetName);

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
                plugin.addPendingHealth(offlinePlayer.getUniqueId(), hearts * LifestealConstants.HEALTH_PER_HEART);

                sender.sendMessage(Component.text("Revived " + targetName + " with " + hearts + " hearts!", NamedTextColor.GREEN));
                return true;
            } else {
                sender.sendMessage(Component.text(targetName + " is not banned/dead!", NamedTextColor.RED));
                return true;
            }
        }

        return false;
    }
}
