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

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.Mudzya>> MUDZYA = REGISTER.register("mudzya",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.Mudzya::new, MobCategory.CREATURE)
                    .sized(0.55F, 0.65F).clientTrackingRange(8).build(ArcaneArtifacts.MODID + ":mudzya"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.Gribushka>> GRIBUSHKA = REGISTER.register("gribushka",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.Gribushka::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.1F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":gribushka"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.Amanitoid>> AMANITOID = REGISTER.register("amanitoid",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.Amanitoid::new, MobCategory.CREATURE)
                    .sized(0.8F, 1.5F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":amanitoid"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.spore.SporeCloud>> SPORE_CLOUD = REGISTER.register("spore_cloud",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.spore.SporeCloud>of(com.soulinchains.arcaneartifacts.spore.SporeCloud::new, MobCategory.MISC)
                    .sized(6.0F, 0.5F).fireImmune().noSave().clientTrackingRange(10).updateInterval(Integer.MAX_VALUE)
                    .build(ArcaneArtifacts.MODID + ":spore_cloud"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.pseudo.BulletEntity>> BULLET = REGISTER.register("bullet",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.pseudo.BulletEntity>of(com.soulinchains.arcaneartifacts.pseudo.BulletEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).noSave().clientTrackingRange(8).updateInterval(1)
                    .build(ArcaneArtifacts.MODID + ":bullet"));

    private ModEntities() {
    }
}
