package com.soulinchains.arcaneartifacts.catto;

import com.google.common.collect.ImmutableSet;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.mixin.StructureTemplatePoolAccessor;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Where village Cattos live: little market stalls, storerooms and junk piles added to the villages' house pools, each
 * with a Catto Cushion. A cushion (also one a player puts down) is a point of interest the village Catto spawner looks
 * for; the Catto that appears there keeps the cushion as its home.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class CattoVillage {
    private CattoVillage() {
    }

    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, ArcaneArtifacts.MODID);

    public static final DeferredBlock<CushionBlock> CUSHION = ModBlocks.BLOCKS.register("catto_cushion",
            () -> new CushionBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.4F).sound(SoundType.WOOL).noOcclusion()));

    public static final DeferredHolder<PoiType, PoiType> CUSHION_POI = POI.register("catto_cushion",
            () -> new PoiType(ImmutableSet.copyOf(CUSHION.get().getStateDefinition().getPossibleStates()), 1, 1));

    /** The pieces and their weight in each village's house pool. */
    public static final List<String> PIECES = List.of("catto_stall", "catto_storage", "catto_junk");
    public static final List<String> VILLAGES = List.of("plains", "desert", "savanna", "snowy", "taiga");
    public static final int WEIGHT = 2;

    /** A low, soft cushion. */
    public static final class CushionBlock extends Block {
        private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0);

        public CushionBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    @SubscribeEvent
    public static void addToVillages(ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        for (String village : VILLAGES) {
            pools.getOptional(ResourceLocation.withDefaultNamespace("village/" + village + "/houses")).ifPresent(pool -> {
                var templates = ((StructureTemplatePoolAccessor) pool).arcaneArtifacts$templates();
                for (String piece : PIECES) {
                    StructurePoolElement element = StructurePoolElement.single(ArcaneArtifacts.MODID + ":village/" + piece)
                            .apply(StructureTemplatePool.Projection.RIGID);
                    for (int i = 0; i < WEIGHT; i++) {
                        templates.add(element);
                    }
                }
            });
        }
    }
}
