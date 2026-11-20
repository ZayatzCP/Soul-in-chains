package com.soulinchains.arcaneartifacts.world.crypt;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.world.village.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A Soul Dungeon: a three-storey prison carved into the rock of a Soul Sand Valley, kept by the Jailer. */
public class SoulDungeonStructure extends Structure {
    public static final MapCodec<SoulDungeonStructure> CODEC = simpleCodec(SoulDungeonStructure::new);

    public SoulDungeonStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SOUL_DUNGEON.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = 36 + context.random().nextInt(20);
        BlockPos origin = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new SoulDungeonPiece(origin))));
    }
}
