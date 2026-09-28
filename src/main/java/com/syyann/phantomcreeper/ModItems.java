package com.syyann.phantomcreeper;

//? if >=26.1 {
/*import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
*///?} else {
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//?}
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
//? if >=1.21.11 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?}

public final class ModItems {
    // 1.21.5 之前刷怪蛋用两种颜色染色（底色苦力怕绿，斑点幻翼蓝）；之后每种刷怪蛋有自己的贴图
    //? if >=1.21.11 {
    /*public static final Item PHANTOM_CREEPER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            PhantomCreeperMod.id("phantom_creeper_spawn_egg"),
            new SpawnEggItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, PhantomCreeperMod.id("phantom_creeper_spawn_egg")))
                    .spawnEgg(ModEntities.PHANTOM_CREEPER)));
    *///?} else {
    public static final Item PHANTOM_CREEPER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            PhantomCreeperMod.id("phantom_creeper_spawn_egg"),
            new SpawnEggItem(ModEntities.PHANTOM_CREEPER, 0x0DA70B, 0x43518A, new Item.Properties()));
    //?}

    private ModItems() {
    }

    static void register() {
        // 放进创造模式物品栏的“刷怪蛋”页（26.1 起 Fabric API 的 ItemGroupEvents 改名为 CreativeModeTabEvents）
        //? if >=26.1 {
        /*CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(PHANTOM_CREEPER_SPAWN_EGG));
        *///?} else {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(PHANTOM_CREEPER_SPAWN_EGG));
        //?}
    }
}
