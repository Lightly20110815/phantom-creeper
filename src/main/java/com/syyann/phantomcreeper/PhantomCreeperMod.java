package com.syyann.phantomcreeper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class PhantomCreeperMod implements ModInitializer {
    public static final String MOD_ID = "phantomcreeper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
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
