package com.soulinchains.arcaneartifacts.astral;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The Astral, the White Abyss (1.41-1.42): an endless white plain under a white sky, black monoliths, snow that is
 * not weather, and something that watches. It is reached by the
 * Ascension rite and left through the Astral Gate the rite raises where its participants land; the gate sends each of them
 * back to the place they ascended from.
 */
public final class Astral {
    public static final ResourceKey<Level> ASTRAL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ArcaneArtifacts.MODID, "astral"));
    public static final ResourceLocation EFFECTS = ResourceLocation.fromNamespaceAndPath(ArcaneArtifacts.MODID, "astral");
    /** The top block of the flat plain (rises lift it, see {@link AstralTerrainFeature#ground}). */
    public static final int FLOOR = 63;
    /** How far the landing search goes from the point under the rite, in blocks. */
    static final int SEARCH = 96;

    /** Where a player ascended from; {@code set} is false until the first ascension. */
    public record Origin(ResourceKey<Level> dimension, BlockPos pos, boolean set) {
        public static final Origin NONE = new Origin(Level.OVERWORLD, BlockPos.ZERO, false);
        public static final Codec<Origin> CODEC = RecordCodecBuilder.create(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Origin::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Origin::pos),
                Codec.BOOL.fieldOf("set").forGetter(Origin::set)).apply(i, Origin::new));
    }

    private Astral() {
    }

    public static boolean isAstral(Level level) {
        return level.dimension() == ASTRAL;
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(ASTRAL);
    }

    /** Whether a player could stand on the plain at this column: no monolith there, open sky over it. */
    static boolean open(ServerLevel astral, int x, int z) {
        int g = AstralTerrainFeature.ground(astral.getSeed(), x, z);
        return !astral.getBlockState(new BlockPos(x, g + 1, z)).is(ModBlocks.ASTRAL_MONOLITH.get())
                && astral.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= g + 2;
    }

    /** Where one stands at (x, z): on the ground, on top of any snow. */
    static int standY(ServerLevel astral, int x, int z) {
        return AstralTerrainFeature.ground(astral.getSeed(), x, z) + 1 + (AstralTerrainFeature.snow(astral.getSeed(), x, z) > 0 ? 1 : 0);
    }

    /**
     * The nearest open spot on the plain to (x, z) with room for a ring of players around it (3 blocks every way), searched
     * in widening squares; the point itself when nothing is found.
     */
    public static BlockPos landing(ServerLevel astral, int x, int z) {
        for (int r = 0; r <= SEARCH; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int cx = x + dx;
                    int cz = z + dz;
                    if (open(astral, cx, cz) && open(astral, cx + 3, cz + 3) && open(astral, cx - 3, cz - 3)
                            && open(astral, cx + 3, cz - 3) && open(astral, cx - 3, cz + 3)) {
                        return new BlockPos(cx, AstralTerrainFeature.ground(astral.getSeed(), cx, cz) + 1, cz);
                    }
                }
            }
        }
        return new BlockPos(x, AstralTerrainFeature.ground(astral.getSeed(), x, z) + 1, z);
    }

    /**
     * Sends the players of an Ascension rite into the Astral: a gate rises on the nearest open spot under the site, they
     * stand in a ring around it, and each remembers where they ascended from. Returns the gate, or null without an Astral.
     */
    @Nullable
    public static BlockPos ascend(ServerLevel from, BlockPos site, Collection<? extends Player> players) {
        ServerLevel astral = level(from.getServer());
        if (astral == null) {
            return null;
        }
        BlockPos gate = landing(astral, site.getX(), site.getZ());
        if (!astral.getBlockState(gate).is(ModBlocks.ASTRAL_GATE.get())) {
            astral.setBlock(gate, ModBlocks.ASTRAL_GATE.get().defaultBlockState(), 3);
        }
        List<ServerPlayer> going = new ArrayList<>();
        for (Player p : players) {
            if (p instanceof ServerPlayer sp && sp.isAlive()) {
                going.add(sp);
            }
        }
        for (int i = 0; i < going.size(); i++) {
            ServerPlayer p = going.get(i);
            p.setData(ModAttachments.ASTRAL_ORIGIN, new Origin(from.dimension(), p.blockPosition(), true));
            double angle = Math.PI * 2 * i / going.size();
            double x = gate.getX() + 0.5 + Math.cos(angle) * 2.5;
            double z = gate.getZ() + 0.5 + Math.sin(angle) * 2.5;
            float yaw = (float) (Mth.atan2(gate.getZ() + 0.5 - z, gate.getX() + 0.5 - x) * Mth.RAD_TO_DEG) - 90F;
            p.teleportTo(astral, x, standY(astral, Mth.floor(x), Mth.floor(z)), z, yaw, 0F);
            p.displayClientMessage(Component.translatable("arcane_artifacts.astral.arrived"), true);
        }
        astral.playSound(null, gate, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2F, 0.5F);
        astral.sendParticles(ParticleTypes.END_ROD, gate.getX() + 0.5, gate.getY() + 1.5, gate.getZ() + 0.5, 60, 1.5, 1.5, 1.5, 0.02);
        return gate;
    }

    /**
     * Sends a player out of the Astral: to where they ascended from, or, without that, to their spawn point (or the world
     * spawn). Returns whether they went.
     */
    public static boolean leave(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        Origin origin = player.getData(ModAttachments.ASTRAL_ORIGIN);
        ServerLevel target = origin.set() ? server.getLevel(origin.dimension()) : null;
        BlockPos pos;
        if (target == null || target.dimension() == ASTRAL) {
            target = server.overworld();
            BlockPos spawn = target.getSharedSpawnPos();
            pos = target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
        } else {
            pos = origin.pos();
        }
        player.setData(ModAttachments.ASTRAL_ORIGIN, Origin.NONE);
        player.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        target.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.4F);
        return true;
    }
}
