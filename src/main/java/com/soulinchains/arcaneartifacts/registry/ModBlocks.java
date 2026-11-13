package com.soulinchains.arcaneartifacts.registry;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.block.ResearchTableBlock;
import com.soulinchains.arcaneartifacts.block.ResearchTableBlockEntity;
import com.soulinchains.arcaneartifacts.block.ResearchTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneArtifacts.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArcaneArtifacts.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ArcaneArtifacts.MODID);

    public static final DeferredBlock<ResearchTableBlock> RESEARCH_TABLE = BLOCKS.register("research_table",
            () -> new ResearchTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<net.minecraft.world.level.block.Block> ROSE_GOLD_BLOCK = BLOCKS.register("rose_gold_block",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK)
                    .requiresCorrectToolForDrops().strength(5.0f, 6.0f).sound(SoundType.METAL)));

    // ------------------------------------------------------------------ Mushroom Caves
    public static final DeferredBlock<net.minecraft.world.level.block.Block> MUSHROOM_MOSS = BLOCKS.register("mushroom_moss",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.6f).sound(SoundType.MOSS).lightLevel(state -> 2)));
    public static final DeferredBlock<net.minecraft.world.level.block.HugeMushroomBlock> BLUE_MUSHROOM_BLOCK = BLOCKS.register("blue_mushroom_block",
            () -> new net.minecraft.world.level.block.HugeMushroomBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> 7)));
    public static final java.util.Map<com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom, DeferredBlock<com.soulinchains.arcaneartifacts.block.ArcaneMushroomBlock>> MUSHROOMS =
            mushrooms();

    private static java.util.Map<com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom, DeferredBlock<com.soulinchains.arcaneartifacts.block.ArcaneMushroomBlock>> mushrooms() {
        var map = new java.util.EnumMap<com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom, DeferredBlock<com.soulinchains.arcaneartifacts.block.ArcaneMushroomBlock>>(
                com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom.class);
        for (var mushroom : com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom.values()) {
            map.put(mushroom, BLOCKS.register(mushroom.id(), () -> new com.soulinchains.arcaneartifacts.block.ArcaneMushroomBlock(
                    com.soulinchains.arcaneartifacts.world.ModFeatures.HUGE_BLUE_MUSHROOM_KEY, mushroom.growsHuge(),
                    BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM)
                            .mapColor(mushroom.mapColor()).lightLevel(state -> mushroom.light()))));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    // ------------------------------------------------------------------ Mudzya Hive and Alchemical Distiller
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.block.MudzyaHiveBlock> MUDZYA_HIVE = BLOCKS.register("mudzya_hive",
            () -> new com.soulinchains.arcaneartifacts.block.MudzyaHiveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6f).sound(SoundType.WOOD).lightLevel(state -> 4)));
    /** The hive Mudzyas build themselves on beaches. */
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.block.MudzyaHiveBlock> WILD_MUDZYA_HIVE = BLOCKS.register("wild_mudzya_hive",
            () -> new com.soulinchains.arcaneartifacts.block.MudzyaHiveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6f).sound(SoundType.WOOD).lightLevel(state -> 6)));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.alchemy.AlchemicalDistillerBlock> ALCHEMICAL_DISTILLER = BLOCKS.register("alchemical_distiller",
            () -> new com.soulinchains.arcaneartifacts.alchemy.AlchemicalDistillerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f).sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 5)));

    public static final DeferredBlock<com.soulinchains.arcaneartifacts.block.AmberClusterBlock> AMBER_CLUSTER = BLOCKS.register("amber_cluster",
            () -> new com.soulinchains.arcaneartifacts.block.AmberClusterBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SMALL_AMETHYST_BUD)
                    .mapColor(MapColor.COLOR_ORANGE).lightLevel(state -> 3)));

    // ------------------------------------------------------------------ Pseudomage logistics
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.pseudo.ManaTankBlock> MANA_TANK = BLOCKS.register("mana_tank",
            () -> new com.soulinchains.arcaneartifacts.pseudo.ManaTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f).sound(SoundType.COPPER).noOcclusion().requiresCorrectToolForDrops()));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.pseudo.ManaPipeBlock> MANA_PIPE = BLOCKS.register("mana_pipe",
            () -> new com.soulinchains.arcaneartifacts.pseudo.ManaPipeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.0f).sound(SoundType.COPPER).noOcclusion()));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.pseudo.ManaPipeBlockEntity>> MANA_PIPE_ENTITY =
            BLOCK_ENTITIES.register("mana_pipe", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.pseudo.ManaPipeBlockEntity::new, MANA_PIPE.get()).build(null));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock> MUSHROOM_TABLE = BLOCKS.register("mushroom_table",
            () -> new com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock(com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock.Kind.TABLE,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock> MUSHROOM_STOOL = BLOCKS.register("mushroom_stool",
            () -> new com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock(com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock.Kind.STOOL,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock> MUSHROOM_LAMP = BLOCKS.register("mushroom_lamp",
            () -> new com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock(com.soulinchains.arcaneartifacts.block.MushroomFurnitureBlock.Kind.LAMP,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5f).sound(SoundType.SHROOMLIGHT).noOcclusion()
                            .lightLevel(state -> 14)));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.pseudo.ManaPumpBlock> MANA_PUMP = BLOCKS.register("mana_pump",
            () -> new com.soulinchains.arcaneartifacts.pseudo.ManaPumpBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f).sound(SoundType.COPPER).noOcclusion().requiresCorrectToolForDrops()));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.pseudo.ManaPumpBlockEntity>> MANA_PUMP_ENTITY =
            BLOCK_ENTITIES.register("mana_pump", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.pseudo.ManaPumpBlockEntity::new, MANA_PUMP.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.pseudo.ManaTankBlockEntity>> MANA_TANK_ENTITY =
            BLOCK_ENTITIES.register("mana_tank", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.pseudo.ManaTankBlockEntity::new, MANA_TANK.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.block.MudzyaHiveBlockEntity>> MUDZYA_HIVE_ENTITY =
            BLOCK_ENTITIES.register("mudzya_hive", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.block.MudzyaHiveBlockEntity::new, MUDZYA_HIVE.get(), WILD_MUDZYA_HIVE.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.alchemy.AlchemicalDistillerBlockEntity>> ALCHEMICAL_DISTILLER_ENTITY =
            BLOCK_ENTITIES.register("alchemical_distiller", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.alchemy.AlchemicalDistillerBlockEntity::new, ALCHEMICAL_DISTILLER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<com.soulinchains.arcaneartifacts.alchemy.AlchemicalDistillerMenu>> ALCHEMICAL_DISTILLER_MENU =
            MENUS.register("alchemical_distiller", () -> new MenuType<>(com.soulinchains.arcaneartifacts.alchemy.AlchemicalDistillerMenu::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE_ENTITY =
            BLOCK_ENTITIES.register("research_table", () -> BlockEntityType.Builder.of(ResearchTableBlockEntity::new, RESEARCH_TABLE.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchTableMenu>> RESEARCH_TABLE_MENU =
            MENUS.register("research_table", () -> IMenuTypeExtension.create(ResearchTableMenu::new));

    private ModBlocks() {
    }
}
