package com.syyann.phantomcreeper.client;

import com.syyann.phantomcreeper.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class PhantomCreeperClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.PHANTOM_CREEPER, PhantomCreeperRenderer::new);
    }
}
