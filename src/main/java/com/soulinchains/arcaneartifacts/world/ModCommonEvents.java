package com.soulinchains.arcaneartifacts.world;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.alchemy.DistillerIngredient;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import com.soulinchains.arcaneartifacts.entity.Mudzya;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Mod-bus registrations for the creatures and the distiller's data map. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCommonEvents {
    private ModCommonEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MUDZYA.get(), Mudzya.createAttributes().build());
        event.put(ModEntities.RESTLESS.get(), com.soulinchains.arcaneartifacts.necro.mob.Restless.createAttributes().build());
        event.put(ModEntities.SOUL_EATER.get(), com.soulinchains.arcaneartifacts.necro.mob.SoulEater.createAttributes().build());
        event.put(ModEntities.MOURNER.get(), com.soulinchains.arcaneartifacts.necro.mob.Mourner.createAttributes().build());
        ModEntities.SERVANTS.forEach((kind, type) -> event.put(type.get(), com.soulinchains.arcaneartifacts.necro.Servant.createAttributes(kind).build()));
        event.put(ModEntities.GRIBUSHKA.get(), Gribushka.createAttributes().build());
        event.put(ModEntities.AMANITOID.get(), com.soulinchains.arcaneartifacts.entity.Amanitoid.createAttributes().build());
        event.put(ModEntities.ELDER_MYCELINOID.get(), com.soulinchains.arcaneartifacts.entity.ElderMycelinoid.createAttributes().build());
        event.put(ModEntities.GRIBULKA.get(), com.soulinchains.arcaneartifacts.entity.Gribulka.createAttributes().build());
        event.put(ModEntities.GRIBULKA_HIVE.get(), com.soulinchains.arcaneartifacts.entity.GribulkaHive.createAttributes().build());
        event.put(ModEntities.FLYING_BROOM.get(), com.soulinchains.arcaneartifacts.mount.FlyingBroom.createAttributes().build());
        event.put(ModEntities.MOSS_ELK.get(), com.soulinchains.arcaneartifacts.mount.MossElk.createAttributes().build());
        event.put(ModEntities.IRON_HORSE.get(), com.soulinchains.arcaneartifacts.mount.IronHorse.createAttributes().build());
        event.put(com.soulinchains.arcaneartifacts.catto.CattoContent.CATTO.get(), com.soulinchains.arcaneartifacts.catto.Catto.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.GRIBULKA.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.soulinchains.arcaneartifacts.entity.Gribulka::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.GRIBULKA_HIVE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.soulinchains.arcaneartifacts.entity.GribulkaHive::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MUDZYA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mudzya::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void dataMaps(RegisterDataMapTypesEvent event) {
        event.register(DistillerIngredient.DATA_MAP);
    }
}
