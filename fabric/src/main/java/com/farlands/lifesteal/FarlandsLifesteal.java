package com.farlands.lifesteal;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FarlandsLifesteal implements ModInitializer {
	public static final String MOD_ID = "farlands-lifesteal";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Farlands Lifesteal...");
	}

	public static Identifier id(@NonNull String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
