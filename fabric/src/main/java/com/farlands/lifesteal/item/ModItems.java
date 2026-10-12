package com.farlands.lifesteal.item;

import com.farlands.lifesteal.FarlandsLifesteal;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import net.minecraft.resources.ResourceKey;

public class ModItems {
    @SuppressWarnings("null")
    public static final Item HEART = new Item(new Item.Properties()
            .setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, FarlandsLifesteal.id("heart"))));

    @SuppressWarnings("null")
    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, FarlandsLifesteal.id("heart"), HEART);
    }
}
