package com.soulinchains.arcaneartifacts.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Mycelinoids' building wood: three sets of mushroom planks (red, brown and blue caps), each with
 * stairs, slab, fence, fence gate, door, trapdoor, button and pressure plate, like the nether woods;
 * and a carpet of mushroom moss.
 */
public final class MushroomWood {
    public enum Kind {
        RED("red_mushroom", MapColor.COLOR_RED),
        BROWN("brown_mushroom", MapColor.COLOR_BROWN),
        BLUE("blue_mushroom", MapColor.COLOR_LIGHT_BLUE);

        public final String prefix;
        final MapColor color;

        Kind(String prefix, MapColor color) {
            this.prefix = prefix;
            this.color = color;
        }
    }

    /** One set of mushroom wood. */
    public static final class Set {
        public final Kind kind;
        public final DeferredBlock<Block> planks;
        public final DeferredBlock<StairBlock> stairs;
        public final DeferredBlock<SlabBlock> slab;
        public final DeferredBlock<FenceBlock> fence;
        public final DeferredBlock<FenceGateBlock> fenceGate;
        public final DeferredBlock<DoorBlock> door;
        public final DeferredBlock<TrapDoorBlock> trapdoor;
        public final DeferredBlock<ButtonBlock> button;
        public final DeferredBlock<PressurePlateBlock> pressurePlate;
        public final List<DeferredItem<? extends Item>> items = new ArrayList<>();

        Set(Kind kind) {
            this.kind = kind;
            String p = kind.prefix;
            BlockBehaviour.Properties wood = BlockBehaviour.Properties.of().mapColor(kind.color).instrument(NoteBlockInstrument.BASS)
                    .strength(2.0f, 3.0f).sound(SoundType.NETHER_WOOD);
            planks = ModBlocks.BLOCKS.register(p + "_planks", () -> new Block(wood));
            stairs = ModBlocks.BLOCKS.register(p + "_stairs", () -> new StairBlock(planks.get().defaultBlockState(), wood));
            slab = ModBlocks.BLOCKS.register(p + "_slab", () -> new SlabBlock(wood));
            fence = ModBlocks.BLOCKS.register(p + "_fence", () -> new FenceBlock(wood));
            fenceGate = ModBlocks.BLOCKS.register(p + "_fence_gate", () -> new FenceGateBlock(WoodType.CRIMSON, wood));
            door = ModBlocks.BLOCKS.register(p + "_door", () -> new DoorBlock(BlockSetType.CRIMSON,
                    BlockBehaviour.Properties.of().mapColor(kind.color).strength(3.0f).noOcclusion().pushReaction(PushReaction.DESTROY)
                            .sound(SoundType.NETHER_WOOD)));
            trapdoor = ModBlocks.BLOCKS.register(p + "_trapdoor", () -> new TrapDoorBlock(BlockSetType.CRIMSON,
                    BlockBehaviour.Properties.of().mapColor(kind.color).strength(3.0f).noOcclusion().isValidSpawn((s, l, pos, t) -> false)
                            .sound(SoundType.NETHER_WOOD)));
            button = ModBlocks.BLOCKS.register(p + "_button", () -> new ButtonBlock(BlockSetType.CRIMSON, 30,
                    BlockBehaviour.Properties.of().noCollission().strength(0.5f).pushReaction(PushReaction.DESTROY)));
            pressurePlate = ModBlocks.BLOCKS.register(p + "_pressure_plate", () -> new PressurePlateBlock(BlockSetType.CRIMSON,
                    BlockBehaviour.Properties.of().mapColor(kind.color).noCollission().strength(0.5f).pushReaction(PushReaction.DESTROY)));
            for (DeferredBlock<? extends Block> block : List.of(planks, stairs, slab, fence, fenceGate, trapdoor, button, pressurePlate)) {
                items.add(ModItems.REGISTER.registerSimpleBlockItem(block));
            }
            items.add(5, ModItems.REGISTER.register(p + "_door", () -> new DoubleHighBlockItem(door.get(), new Item.Properties())));
        }
    }

    public static final Map<Kind, Set> SETS = new EnumMap<>(Kind.class);

    public static final DeferredBlock<CarpetBlock> MUSHROOM_MOSS_CARPET = ModBlocks.BLOCKS.register("mushroom_moss_carpet",
            () -> new CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.1f)
                    .sound(SoundType.MOSS_CARPET).lightLevel(state -> 2).pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BlockItem> MUSHROOM_MOSS_CARPET_ITEM = ModItems.REGISTER.registerSimpleBlockItem(MUSHROOM_MOSS_CARPET);

    static {
        for (Kind kind : Kind.values()) {
            SETS.put(kind, new Set(kind));
        }
    }

    private MushroomWood() {
    }

    /** Called from the mod constructor, before the registers are attached, so everything above is registered. */
    public static void init() {
    }
}
