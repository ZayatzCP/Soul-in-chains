package com.soulinchains.arcaneartifacts.astral;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The Astral's presences: now and then a Watcher stands on the top of a nearby monolith, facing a traveller. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class AstralEvents {
    /** How often a traveller may be watched: checked every {@link #CHECK} ticks with this chance. */
    static final int CHECK = 40;
    static final float CHANCE = 1F / 45F;
    /** How far, sideways, the monolith may be, and how much higher than the traveller its top. */
    static final double MIN_DISTANCE = 40;
    static final double MAX_DISTANCE = 140;
    static final int MAX_RISE = 90;

    private static final Map<UUID, Watcher> WATCHED = new java.util.HashMap<>();

    private AstralEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level) || !Astral.isAstral(level)
                || player.isSpectator() || player.tickCount % CHECK != 0) {
            return;
        }
        Watcher current = WATCHED.get(player.getUUID());
        if (current != null && !current.isRemoved()) {
            return;
        }
        WATCHED.remove(player.getUUID());
        if (level.random.nextFloat() < CHANCE) {
            Watcher watcher = summon(level, player);
            if (watcher != null) {
                WATCHED.put(player.getUUID(), watcher);
            }
        }
    }

    /** The top of a monolith fit for a Watcher near the player: a tall or sunk one, far enough, its top in sight. */
    @Nullable
    public static BlockPos perch(ServerLevel level, Player player) {
        int cell = AstralTerrainFeature.CELL;
        int pcx = Math.floorDiv(player.getBlockX(), cell);
        int pcz = Math.floorDiv(player.getBlockZ(), cell);
        List<BlockPos> tops = new ArrayList<>();
        for (int cx = pcx - 2; cx <= pcx + 2; cx++) {
            for (int cz = pcz - 2; cz <= pcz + 2; cz++) {
                AstralTerrainFeature.Plan plan = AstralTerrainFeature.plan(level.getSeed(), cx, cz, level.getMaxBuildHeight());
                if (plan == null || plan.kind() == AstralTerrainFeature.Kind.WHOLE) {
                    continue;
                }
                int y = plan.top() - 1;
                BlockPos top = new BlockPos(plan.centerX(y), y, plan.centerZ(y));
                double d = Math.sqrt(player.distanceToSqr(top.getX() + 0.5, player.getY(), top.getZ() + 0.5));
                if (d < MIN_DISTANCE || d > MAX_DISTANCE || top.getY() - player.getY() > MAX_RISE || !level.isLoaded(top)) {
                    continue;
                }
                if (level.getBlockState(top).is(ModBlocks.ASTRAL_MONOLITH.get()) && level.getBlockState(top.above()).isAir()
                        && level.getBlockState(top.above(8)).isAir()) {
                    tops.add(top);
                }
            }
        }
        return tops.isEmpty() ? null : tops.get(level.random.nextInt(tops.size()));
    }

    @Nullable
    public static Watcher summon(ServerLevel level, Player player) {
        BlockPos top = perch(level, player);
        if (top == null) {
            return null;
        }
        Watcher watcher = ModEntities.WATCHER.get().create(level);
        if (watcher == null) {
            return null;
        }
        watcher.standOn(top);
        watcher.watch(player);
        level.addFreshEntity(watcher);
        return watcher;
    }
}
