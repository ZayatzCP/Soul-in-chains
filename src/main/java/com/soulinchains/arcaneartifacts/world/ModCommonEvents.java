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
        event.put(ModEntities.GRIBUSHKA.get(), Gribushka.createAttributes().build());
        event.put(ModEntities.AMANITOID.get(), com.soulinchains.arcaneartifacts.entity.Amanitoid.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.MUDZYA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mudzya::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void dataMaps(RegisterDataMapTypesEvent event) {
        event.register(DistillerIngredient.DATA_MAP);
    }
}
