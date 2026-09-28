package com.syyann.phantomcreeper;

import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=1.21.2 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    /** 碰撞箱 0.9 x 1.6：上半截是幻翼，下半截是吊着的苦力怕 */
    public static final EntityType<PhantomCreeperEntity> PHANTOM_CREEPER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            PhantomCreeperMod.id("phantom_creeper"),
            EntityType.Builder.of(PhantomCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 1.6f)
                    // 眼睛在上方的幻翼身上（1.20.5 之前由实体类的 getStandingEyeHeight 决定）
                    //? if >=1.20.5 {
                    /*.eyeHeight(1.6f * 0.85f)
                    *///?}
                    .clientTrackingRange(8)
                    //? if >=1.21.2 {
                    /*.build(ResourceKey.create(Registries.ENTITY_TYPE, PhantomCreeperMod.id("phantom_creeper"))));
                    *///?} else {
                    .build("phantom_creeper"));
                    //?}

    private ModEntities() {
    }

    static void register() {
        FabricDefaultAttributeRegistry.register(PHANTOM_CREEPER, PhantomCreeperEntity.createPhantomCreeperAttributes());
    }
}
