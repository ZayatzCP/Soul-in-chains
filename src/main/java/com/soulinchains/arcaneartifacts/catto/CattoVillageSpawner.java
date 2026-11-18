package com.soulinchains.arcaneartifacts.catto;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Catto also lives in villages, like the village cats: about once a minute, near each player, a village with at least
 * four occupied beds and no wild Catto within 48 blocks may get one. Only loaded chunks, only with mob spawning on.
 * A village Catto has no owner; a fish may win it over (see {@link Catto}).
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class CattoVillageSpawner {
    public static final int INTERVAL = 1200;
    public static final int RANGE = 48;

    private CattoVillageSpawner() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && level.random.nextInt(3) == 0) {
                trySpawn(level, player.blockPosition());
            }
        }
    }

    /** Tries one spot around {@code near}; returns the new Catto's position, or null. */
    public static BlockPos trySpawn(ServerLevel level, BlockPos near) {
        BlockPos column = near.offset(level.random.nextInt(RANGE) - RANGE / 2, 0, level.random.nextInt(RANGE) - RANGE / 2);
        if (!level.isLoaded(column)) {
            return null;
        }
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (!level.isVillage(pos) || level.getPoiManager().getCountInRange(h -> h.is(PoiTypes.HOME), pos, RANGE,
                PoiManager.Occupancy.IS_OCCUPIED) < 4) {
            return null;
        }
        if (!level.getEntitiesOfClass(Catto.class, new AABB(pos).inflate(RANGE, 16, RANGE), c -> c.getOwnerUUID() == null).isEmpty()) {
            return null;
        }
        return spawnAt(level, pos);
    }

    /** Puts a wild Catto on the ground at {@code pos} if there is room. */
    public static BlockPos spawnAt(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) || !level.getFluidState(pos).isEmpty()) {
            return null;
        }
        Catto cat = CattoContent.CATTO.get().create(level);
        if (cat == null) {
            return null;
        }
        cat.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(cat, cat.getBoundingBox().move(Vec3.ZERO))) {
            return null;
        }
        level.addFreshEntity(cat);
        return pos;
    }
}
