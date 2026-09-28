package com.syyann.phantomcreeper;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    /** 底色苦力怕绿，斑点幻翼蓝 */
    public static final Item PHANTOM_CREEPER_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            PhantomCreeperMod.id("phantom_creeper_spawn_egg"),
            new SpawnEggItem(ModEntities.PHANTOM_CREEPER, 0x0DA70B, 0x43518A, new Item.Settings()));

    private ModItems() {
    }

    static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(PHANTOM_CREEPER_SPAWN_EGG));
    }
}
