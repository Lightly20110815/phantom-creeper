package com.syyann.phantomcreeper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

public class PhantomCreeperMod implements ModInitializer {
    public static final String MOD_ID = "phantomcreeper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        *///?} else {
        return new ResourceLocation(MOD_ID, path);
        //?}
    }

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModItems.register();
        ModGameRules.register();
        PhantomCreeperSpawner.register();
        LOGGER.info("Phantom Creeper loaded");
    }
}
