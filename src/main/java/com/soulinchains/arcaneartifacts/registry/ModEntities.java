package com.soulinchains.arcaneartifacts.registry;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, ArcaneArtifacts.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneMissileEntity>> ARCANE_MISSILE = REGISTER.register("arcane_missile",
            () -> EntityType.Builder.<ArcaneMissileEntity>of(ArcaneMissileEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArcaneArtifacts.MODID + ":arcane_missile"));

    private ModEntities() {
    }
}
