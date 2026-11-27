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

    // ------------------------------------------------------------------ Shackled: graveyard soil
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.necro.GraveSoilBlock> CONSECRATED_SOIL = BLOCKS.register("consecrated_soil",
            () -> new com.soulinchains.arcaneartifacts.necro.GraveSoilBlock(true));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.necro.GraveSoilBlock> DESECRATED_SOIL = BLOCKS.register("desecrated_soil",
            () -> new com.soulinchains.arcaneartifacts.necro.GraveSoilBlock(false));

    public static final DeferredBlock<com.soulinchains.arcaneartifacts.necro.GraveBlock> GRAVE = BLOCKS.register("grave",
            com.soulinchains.arcaneartifacts.necro.GraveBlock::new);

    // ------------------------------------------------------------------ Shackled: Throne of Chains
    public static final DeferredBlock<net.minecraft.world.level.block.Block> SHACKLE_STONE = BLOCKS.register("shackle_stone",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops().strength(2.5f, 9.0f).sound(SoundType.POLISHED_DEEPSLATE)));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.necro.throne.ChainPillarBlock> CHAIN_PILLAR = BLOCKS.register("chain_pillar", com.soulinchains.arcaneartifacts.necro.throne.ChainPillarBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.necro.throne.EmptyThroneBlock> EMPTY_THRONE = BLOCKS.register("empty_throne", com.soulinchains.arcaneartifacts.necro.throne.EmptyThroneBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.necro.throne.GreatSoulLanternBlock> GREAT_SOUL_LANTERN = BLOCKS.register("great_soul_lantern", com.soulinchains.arcaneartifacts.necro.throne.GreatSoulLanternBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.necro.throne.ChainLockBlock> CHAIN_LOCK = BLOCKS.register("chain_lock", com.soulinchains.arcaneartifacts.necro.throne.ChainLockBlock::new);

    // ------------------------------------------------------------------ Chalk rites (the Cultist)
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.rite.ChalkMarkBlock> CHALK_MARK = BLOCKS.register("chalk_mark", com.soulinchains.arcaneartifacts.rite.ChalkMarkBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.rite.RiteHeartBlock> RITE_HEART = BLOCKS.register("rite_heart", com.soulinchains.arcaneartifacts.rite.RiteHeartBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.rite.OfferingPedestalBlock> OFFERING_PEDESTAL = BLOCKS.register("offering_pedestal", com.soulinchains.arcaneartifacts.rite.OfferingPedestalBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.rite.RitualCenserBlock> RITUAL_CENSER = BLOCKS.register("ritual_censer", com.soulinchains.arcaneartifacts.rite.RitualCenserBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.rite.RiteCageBlock> RITE_CAGE = BLOCKS.register("rite_cage", com.soulinchains.arcaneartifacts.rite.RiteCageBlock::new);
    // the Astral (1.41)
    // the Astral (1.41): its plain and its monoliths cannot be broken
    public static final DeferredBlock<net.minecraft.world.level.block.Block> ASTRAL_STONE = BLOCKS.registerSimpleBlock("astral_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.CALCITE)
                    .isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> ASTRAL_MONOLITH = BLOCKS.registerSimpleBlock("astral_monolith",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE)
                    .isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.astral.AstralSnowBlock> ASTRAL_SNOW = BLOCKS.register("astral_snow", com.soulinchains.arcaneartifacts.astral.AstralSnowBlock::new);
    // the Thicket Lights (1.44)
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.grove.LightJarBlock> LIGHT_JAR = BLOCKS.register("light_jar", com.soulinchains.arcaneartifacts.grove.LightJarBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.grove.LightCageBlock> LIGHT_CAGE = BLOCKS.register("light_cage", com.soulinchains.arcaneartifacts.grove.LightCageBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.grove.LightJarBlockEntity>> LIGHT_JAR_ENTITY =
            BLOCK_ENTITIES.register("light_jar", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.grove.LightJarBlockEntity::new, LIGHT_JAR.get()).build(null));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.astral.AstralGateBlock> ASTRAL_GATE = BLOCKS.register("astral_gate", com.soulinchains.arcaneartifacts.astral.AstralGateBlock::new);
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.rite.CorruptedSoilBlock> CORRUPTED_SOIL = BLOCKS.register("corrupted_soil", com.soulinchains.arcaneartifacts.rite.CorruptedSoilBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.rite.RiteHeartBlockEntity>> RITE_HEART_ENTITY =
            BLOCK_ENTITIES.register("rite_heart", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.rite.RiteHeartBlockEntity::new, RITE_HEART.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.rite.OfferingPedestalBlockEntity>> OFFERING_PEDESTAL_ENTITY =
            BLOCK_ENTITIES.register("offering_pedestal", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.rite.OfferingPedestalBlockEntity::new, OFFERING_PEDESTAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.soulinchains.arcaneartifacts.rite.RiteCageBlockEntity>> RITE_CAGE_ENTITY =
            BLOCK_ENTITIES.register("rite_cage", () -> BlockEntityType.Builder.of(com.soulinchains.arcaneartifacts.rite.RiteCageBlockEntity::new, RITE_CAGE.get()).build(null));

    // ------------------------------------------------------------------ Archive of Unkept Promises

    // ------------------------------------------------------------------ Mushroom Caves
    public static final DeferredBlock<net.minecraft.world.level.block.Block> MUSHROOM_MOSS = BLOCKS.register("mushroom_moss",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.6f).sound(SoundType.MOSS).lightLevel(state -> 3)));
    public static final DeferredBlock<net.minecraft.world.level.block.HugeMushroomBlock> BLUE_MUSHROOM_BLOCK = BLOCKS.register("blue_mushroom_block",
            () -> new com.soulinchains.arcaneartifacts.block.Bouncy.BouncyHugeMushroomBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> 7)));
    /** Blue mushroom variants: springy like the cap itself. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> SPOTTED_BLUE_MUSHROOM_BLOCK = BLOCKS.register("spotted_blue_mushroom_block",
            () -> new com.soulinchains.arcaneartifacts.block.Bouncy.BouncyBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> 7)));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> GLOWING_BLUE_MUSHROOM_BLOCK = BLOCKS.register("glowing_blue_mushroom_block",
            () -> new com.soulinchains.arcaneartifacts.block.Bouncy.BouncyBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> 14)));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> BLUE_MUSHROOM_GILLS = BLOCKS.register("blue_mushroom_gills",
            () -> new com.soulinchains.arcaneartifacts.block.Bouncy.BouncyBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK)
                    .mapColor(MapColor.COLOR_BLUE).lightLevel(state -> 4)));
    /** Small plants of the Mushroom Caves. */
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.cave.CavePlantBlock> SPORE_TUFT = BLOCKS.register("spore_tuft",
            () -> new com.soulinchains.arcaneartifacts.cave.CavePlantBlock(false, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .noCollission().instabreak().replaceable().sound(SoundType.NETHER_SPROUTS).lightLevel(state -> 0)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.cave.CavePlantBlock> GLOWSPROUT = BLOCKS.register("glowsprout",
            () -> new com.soulinchains.arcaneartifacts.cave.CavePlantBlock(false, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission().instabreak().replaceable().sound(SoundType.NETHER_SPROUTS).lightLevel(state -> 12)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.cave.CavePlantBlock> HANGING_HYPHAE = BLOCKS.register("hanging_hyphae",
            () -> new com.soulinchains.arcaneartifacts.cave.CavePlantBlock(true, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .noCollission().instabreak().replaceable().sound(SoundType.HANGING_ROOTS).lightLevel(state -> 7)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    /** The ground of the Mushroom Caves. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> SPORE_CLAY = BLOCKS.register("spore_clay",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CLAY)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)));
    /** Spore clay bricks: raw (crafted), fired, tiles, chiseled, polished; stairs, slab and wall. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> RAW_SPORE_BRICKS = BLOCKS.register("raw_spore_bricks",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.PACKED_MUD)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> SPORE_BRICKS = brick("spore_bricks");
    public static final DeferredBlock<net.minecraft.world.level.block.Block> SPORE_BRICK_TILES = brick("spore_brick_tiles");
    public static final DeferredBlock<net.minecraft.world.level.block.Block> CHISELED_SPORE_BRICKS = brick("chiseled_spore_bricks");
    public static final DeferredBlock<net.minecraft.world.level.block.Block> POLISHED_SPORE_CLAY = brick("polished_spore_clay");
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> SPORE_BRICK_STAIRS = BLOCKS.register("spore_brick_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(SPORE_BRICKS.get().defaultBlockState(), brickProperties()));
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> SPORE_BRICK_SLAB = BLOCKS.register("spore_brick_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(brickProperties()));
    public static final DeferredBlock<net.minecraft.world.level.block.WallBlock> SPORE_BRICK_WALL = BLOCKS.register("spore_brick_wall",
            () -> new net.minecraft.world.level.block.WallBlock(brickProperties().forceSolidOn()));

    private static BlockBehaviour.Properties brickProperties() {
        return BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.MUD_BRICKS).mapColor(MapColor.TERRACOTTA_WHITE);
    }

    private static DeferredBlock<net.minecraft.world.level.block.Block> brick(String name) {
        return BLOCKS.register(name, () -> new net.minecraft.world.level.block.Block(brickProperties()));
    }
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
    public static final DeferredBlock<com.soulinchains.arcaneartifacts.block.AmberClusterBlock> LARGE_AMBER_CLUSTER = BLOCKS.register("large_amber_cluster",
            () -> new com.soulinchains.arcaneartifacts.block.AmberClusterBlock(15.0F, 2.0F, BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER)
                    .mapColor(MapColor.COLOR_ORANGE).lightLevel(state -> 6)));
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> AMBER_BLOCK = BLOCKS.register("amber_block",
            () -> new net.minecraft.world.level.block.TransparentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5f).sound(SoundType.AMETHYST).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 2)
                    .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)));

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

    public static final DeferredBlock<com.soulinchains.arcaneartifacts.pseudo.GunsmithBenchBlock> GUNSMITH_BENCH = BLOCKS.register("gunsmith_bench",
            () -> new com.soulinchains.arcaneartifacts.pseudo.GunsmithBenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5f).sound(SoundType.WOOD)));
    public static final DeferredHolder<MenuType<?>, MenuType<com.soulinchains.arcaneartifacts.pseudo.GunsmithMenu>> GUNSMITH_MENU =
            MENUS.register("gunsmith_bench", () -> new MenuType<>(com.soulinchains.arcaneartifacts.pseudo.GunsmithMenu::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchTableMenu>> RESEARCH_TABLE_MENU =
            MENUS.register("research_table", () -> IMenuTypeExtension.create(ResearchTableMenu::new));

    private ModBlocks() {
    }
}
