package com.farlands.lifesteal.paper;

import com.farlands.lifesteal.paper.command.PaperCommands;
import com.farlands.lifesteal.paper.event.PaperEventListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class FarlandsLifestealPlugin extends JavaPlugin {

    private static FarlandsLifestealPlugin instance;
    private final Map<UUID, Location> pendingTeleports = new HashMap<>();
    private final Map<UUID, Double> pendingHealth = new HashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;
    private boolean folia;

    public static FarlandsLifestealPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        folia = checkFolia();

        getLogger().info("Initializing Farlands Lifesteal (Platform: " + (folia ? "Folia" : "Paper") + ")...");

        loadPluginData();

        // Register commands
        PaperCommands commands = new PaperCommands(this);
        if (getCommand("withdrawheart") != null) getCommand("withdrawheart").setExecutor(commands);
        if (getCommand("removeheart") != null) getCommand("removeheart").setExecutor(commands);
        if (getCommand("revive") != null) getCommand("revive").setExecutor(commands);

        // Register event listener
        getServer().getPluginManager().registerEvents(new PaperEventListener(this), this);
    }

    @Override
    public void onDisable() {
        savePluginData();
    }

    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return folia;
    }

    public void runTask(Entity entity, Runnable runnable) {
        if (folia) {
            entity.getScheduler().run(this, task -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(this, runnable);
        }
    }

    public void runTaskLater(Entity entity, Runnable runnable, long delayTicks) {
        if (folia) {
            entity.getScheduler().runDelayed(this, task -> runnable.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(this, runnable, delayTicks);
        }
    }

    public void runGlobalTask(Runnable runnable) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().run(this, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(this, runnable);
        }
    }

    public void runGlobalTimer(Consumer<Object> taskConsumer, long delayTicks, long periodTicks) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> taskConsumer.accept(task), delayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimer(this, () -> taskConsumer.accept(null), delayTicks, periodTicks);
        }
    }

    // Pending data accessors
    public void addPendingTeleport(UUID uuid, Location location) {
        pendingTeleports.put(uuid, location);
        savePluginData();
    }

    public Location getPendingTeleport(UUID uuid) {
        return pendingTeleports.get(uuid);
    }

    public void removePendingTeleport(UUID uuid) {
        pendingTeleports.remove(uuid);
        savePluginData();
    }

    public boolean hasPendingTeleport(UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }

    public void addPendingHealth(UUID uuid, double health) {
        pendingHealth.put(uuid, health);
        savePluginData();
    }

    public Double getPendingHealth(UUID uuid) {
        return pendingHealth.get(uuid);
    }

    public void removePendingHealth(UUID uuid) {
        pendingHealth.remove(uuid);
        savePluginData();
    }

    public boolean hasPendingHealth(UUID uuid) {
        return pendingHealth.containsKey(uuid);
    }

    private void loadPluginData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("teleports")) {
            for (String key : dataConfig.getConfigurationSection("teleports").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    Location loc = dataConfig.getLocation("teleports." + key);
                    if (loc != null) pendingTeleports.put(uuid, loc);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (dataConfig.contains("health")) {
            for (String key : dataConfig.getConfigurationSection("health").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    pendingHealth.put(uuid, dataConfig.getDouble("health." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void savePluginData() {
        if (dataConfig == null || dataFile == null) return;
        dataConfig.set("teleports", null);
        pendingTeleports.forEach((uuid, loc) -> dataConfig.set("teleports." + uuid, loc));

        dataConfig.set("health", null);
        pendingHealth.forEach((uuid, hp) -> dataConfig.set("health." + uuid, hp));

        try {
            dataConfig.save(dataFile);
        } catch (IOException ignored) {}
    }
}
