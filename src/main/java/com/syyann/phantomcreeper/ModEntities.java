package com.syyann.phantomcreeper;

import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    /** 碰撞箱 0.9 x 1.6：上半截是幻翼，下半截是吊着的苦力怕 */
    public static final EntityType<PhantomCreeperEntity> PHANTOM_CREEPER = Registry.register(
            Registries.ENTITY_TYPE,
            PhantomCreeperMod.id("phantom_creeper"),
            EntityType.Builder.create(PhantomCreeperEntity::new, SpawnGroup.MONSTER)
                    .setDimensions(0.9f, 1.6f)
                    .maxTrackingRange(8)
                    .build("phantom_creeper"));

    private ModEntities() {
    }

    static void register() {
        FabricDefaultAttributeRegistry.register(PHANTOM_CREEPER, PhantomCreeperEntity.createPhantomCreeperAttributes());
    }
}
