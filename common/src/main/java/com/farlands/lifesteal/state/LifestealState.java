package com.farlands.lifesteal.state;

import com.farlands.lifesteal.LifestealConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LifestealState extends SavedData {

    public final Map<UUID, BlockPos> pendingTeleports = new HashMap<>();
    public final Map<UUID, Double> pendingHealth = new HashMap<>();

    @SuppressWarnings("null")
    public static final Codec<LifestealState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, BlockPos.CODEC)
                    .optionalFieldOf("pendingTeleports", new HashMap<>())
                    .forGetter((LifestealState state) -> {
                        Map<String, BlockPos> map = new HashMap<>();
                        state.pendingTeleports.forEach((k, v) -> map.put(k.toString(), v));
                        return map;
                    }),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
                    .optionalFieldOf("pendingHealth", new HashMap<>())
                    .forGetter((LifestealState state) -> {
                        Map<String, Double> map = new HashMap<>();
                        state.pendingHealth.forEach((k, v) -> map.put(k.toString(), v));
                        return map;
                    })
    ).apply(instance, (teleports, healths) -> {
        LifestealState state = new LifestealState();
        teleports.forEach((k, v) -> state.pendingTeleports.put(UUID.fromString(k), v));
        healths.forEach((k, v) -> state.pendingHealth.put(UUID.fromString(k), v));
        return state;
    }));

    @SuppressWarnings("null")
    public static final SavedDataType<LifestealState> TYPE = new SavedDataType<>(
            LifestealConstants.MOD_ID + "_state",
            LifestealState::new,
            CODEC,
            null);

    public void addTeleport(UUID uuid, BlockPos pos) {
        pendingTeleports.put(uuid, pos);
        setDirty();
    }

    public BlockPos getTeleport(UUID uuid) {
        return pendingTeleports.get(uuid);
    }

    public void removeTeleport(UUID uuid) {
        pendingTeleports.remove(uuid);
        setDirty();
    }

    public boolean hasTeleport(UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }

    public void addPendingHealth(UUID uuid, double health) {
        pendingHealth.put(uuid, health);
        setDirty();
    }

    public Double getPendingHealth(UUID uuid) {
        return pendingHealth.get(uuid);
    }

    public void removePendingHealth(UUID uuid) {
        pendingHealth.remove(uuid);
        setDirty();
    }

    public boolean hasPendingHealth(UUID uuid) {
        return pendingHealth.containsKey(uuid);
    }

    @SuppressWarnings("null")
    public static LifestealState getServerState(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null)
            return new LifestealState();

        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void addPendingTeleport(MinecraftServer server, UUID uuid, BlockPos pos) {
        getServerState(server).addTeleport(uuid, pos);
    }

    public static void addPendingHealth(MinecraftServer server, UUID uuid, double health) {
        getServerState(server).addPendingHealth(uuid, health);
    }
}
