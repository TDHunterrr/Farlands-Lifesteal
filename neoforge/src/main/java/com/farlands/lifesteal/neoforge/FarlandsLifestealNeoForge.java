package com.farlands.lifesteal.neoforge;

import com.farlands.lifesteal.LifestealConstants;
import com.farlands.lifesteal.neoforge.event.NeoForgeEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LifestealConstants.NEOFORGE_MOD_ID)
public class FarlandsLifestealNeoForge {
    public static final Logger LOGGER = LoggerFactory.getLogger(LifestealConstants.MOD_ID);

    public FarlandsLifestealNeoForge(IEventBus modEventBus) {
        LOGGER.info("Initializing Farlands Lifesteal for NeoForge...");
        NeoForge.EVENT_BUS.register(new NeoForgeEvents());
    }
}
