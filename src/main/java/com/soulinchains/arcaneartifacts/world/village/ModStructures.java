package com.soulinchains.arcaneartifacts.world.village;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Mycelinoid village structure and its house piece. */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ArcaneArtifacts.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ArcaneArtifacts.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<MycelinoidVillage>> VILLAGE =
            TYPES.register("mycelinoid_village", () -> () -> MycelinoidVillage.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> HOUSE_PIECE =
            PIECES.register("mycelinoid_house", () -> VillageHousePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PATH_PIECE =
            PIECES.register("mycelinoid_path", () -> VillagePathPiece::new);

    private ModStructures() {
    }
}
