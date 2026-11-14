package com.soulinchains.arcaneartifacts.world.village;

import com.soulinchains.arcaneartifacts.entity.Gribushka;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.MushroomWood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** One house of a Mycelinoid village; built chunk by chunk, its inhabitants appear with the chunk of its centre. */
public class VillageHousePiece extends StructurePiece {
    private final BlockPos floor;
    private final BlockPos villageCenter;
    private final HouseType type;
    private final MushroomWood.Kind kind;
    private final Direction door;
    private final long seed;

    public VillageHousePiece(BlockPos floor, BlockPos villageCenter, HouseType type, MushroomWood.Kind kind, Direction door, long seed) {
        super(ModStructures.HOUSE_PIECE.get(), 0, bounds(floor, type));
        this.floor = floor;
        this.villageCenter = villageCenter;
        this.type = type;
        this.kind = kind;
        this.door = door;
        this.seed = seed;
    }

    public VillageHousePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.HOUSE_PIECE.get(), tag);
        this.floor = new BlockPos(tag.getInt("FX"), tag.getInt("FY"), tag.getInt("FZ"));
        this.villageCenter = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.type = HouseType.values()[Math.floorMod(tag.getInt("Type"), HouseType.values().length)];
        this.kind = MushroomWood.Kind.values()[Math.floorMod(tag.getInt("Kind"), MushroomWood.Kind.values().length)];
        this.door = Direction.from2DDataValue(tag.getInt("Door"));
        this.seed = tag.getLong("Seed");
    }

    static BoundingBox bounds(BlockPos floor, HouseType type) {
        int r = type.capRadius();
        return new BoundingBox(floor.getX() - r - 1, floor.getY() - 1, floor.getZ() - r - 1,
                floor.getX() + r + 1, floor.getY() + type.height + 3, floor.getZ() + r + 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("FX", floor.getX());
        tag.putInt("FY", floor.getY());
        tag.putInt("FZ", floor.getZ());
        tag.putInt("CX", villageCenter.getX());
        tag.putInt("CY", villageCenter.getY());
        tag.putInt("CZ", villageCenter.getZ());
        tag.putInt("Type", type.ordinal());
        tag.putInt("Kind", kind.ordinal());
        tag.putInt("Door", door.get2DDataValue());
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        // The same seed in every chunk, so the parts of the house built in different chunks agree.
        new HouseBuilder(level, chunkBox, RandomSource.create(seed)).build(floor, type, kind, door);
        if (chunkBox.isInside(floor)) {
            for (int i = 0; i < type.mycelinoids; i++) {
                spawn(level, ModEntities.GRIBUSHKA.get(), random);
            }
            for (int i = 0; i < type.guards; i++) {
                spawn(level, ModEntities.AMANITOID.get(), random);
            }
        }
    }

    private void spawn(WorldGenLevel level, EntityType<? extends Gribushka> entityType, RandomSource random) {
        Gribushka mob = entityType.create(level.getLevel());
        if (mob == null) {
            return;
        }
        BlockPos at = null;
        if (type == HouseType.WATCHTOWER) {
            at = floor.offset(1, type.height + 1, 0);    // on the lookout
        } else {
            for (int attempt = 0; attempt < 12 && at == null; attempt++) {
                BlockPos spot = floor.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                if (level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir()) {
                    at = spot;
                }
            }
        }
        if (at == null) {
            return;
        }
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360F, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(floor), MobSpawnType.STRUCTURE, null);
        mob.setHome(villageCenter, MycelinoidVillage.RADIUS + 6);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
