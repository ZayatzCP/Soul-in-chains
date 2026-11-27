package com.soulinchains.arcaneartifacts.geo;

import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.common.datagen.BlockTagProvider;
import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The Stone Hoard (1.43): up to {@link #MAX} real blocks torn from the ground by Earthen Surge, circling their owner, spent
 * by the other stone glyphs. It is kept with the player; on death it falls to the ground as items. While Stone Dominion
 * holds, the glyphs take from it without spending anything.
 */
public final class StoneHoard {
    public static final int MAX = 48;
    /** Only terrain can be torn up: stone, earth, sand and their kin (the tag), never buildings, containers or bedrock. */
    public static final TagKey<Block> TEARABLE = TagKey.create(Registries.BLOCK, ArcaneArtifacts.id("tearable"));
    public static final float MAX_HARDNESS = 3.5F;
    public static final Codec<List<BlockState>> CODEC = BlockState.CODEC.listOf().xmap(ArrayList::new, l -> l);
    private static final String DOMINION = ArcaneArtifacts.MODID + ":stone_dominion_until";

    private StoneHoard() {
    }

    public static List<BlockState> of(Player player) {
        return player.getData(ModAttachments.STONE_HOARD);
    }

    public static int count(Player player) {
        return of(player).size();
    }

    public static int space(Player player) {
        return MAX - count(player);
    }

    /** Puts a block into the hoard; false when it is full. */
    public static boolean add(Player player, BlockState state) {
        List<BlockState> hoard = of(player);
        if (hoard.size() >= MAX) {
            return false;
        }
        hoard.add(state.getBlock().defaultBlockState());
        player.setData(ModAttachments.STONE_HOARD, hoard);
        return true;
    }

    /** Whether the player holds at least {@code n} blocks (always, during Stone Dominion, if there is any at all). */
    public static boolean has(Player player, int n) {
        return count(player) >= n || dominion(player) && count(player) > 0;
    }

    /**
     * Takes up to {@code n} blocks from the hoard (the newest first) and returns them. During Stone Dominion the blocks are
     * returned but stay in the hoard.
     */
    public static List<BlockState> take(Player player, int n) {
        List<BlockState> hoard = of(player);
        List<BlockState> out = new ArrayList<>();
        if (dominion(player)) {
            for (int i = 0; i < n && !hoard.isEmpty(); i++) {
                out.add(hoard.get(hoard.size() - 1 - i % hoard.size()));
            }
            return out;
        }
        for (int i = 0; i < n && !hoard.isEmpty(); i++) {
            out.add(hoard.remove(hoard.size() - 1));
        }
        player.setData(ModAttachments.STONE_HOARD, hoard);
        return out;
    }

    /** Takes up to {@code n} blocks for real, even during Stone Dominion (for what stands in the world as blocks). */
    public static List<BlockState> takeReal(Player player, int n) {
        List<BlockState> hoard = of(player);
        List<BlockState> out = new ArrayList<>();
        for (int i = 0; i < n && !hoard.isEmpty(); i++) {
            out.add(hoard.remove(hoard.size() - 1));
        }
        player.setData(ModAttachments.STONE_HOARD, hoard);
        return out;
    }

    public static void clear(Player player) {
        player.setData(ModAttachments.STONE_HOARD, new ArrayList<>());
    }

    // ------------------------------------------------------------------ Stone Dominion

    public static boolean dominion(Player player) {
        return player.getPersistentData().getLong(DOMINION) > player.level().getGameTime();
    }

    public static void grantDominion(Player player, int ticks) {
        player.getPersistentData().putLong(DOMINION, player.level().getGameTime() + ticks);
    }

    public static int dominionTicks(Player player) {
        return (int) Math.max(0, player.getPersistentData().getLong(DOMINION) - player.level().getGameTime());
    }

    // ------------------------------------------------------------------ tearing

    /** Whether the caster may tear this block out of the world into a hoard. */
    public static boolean tearable(ServerLevel level, BlockPos pos, LivingEntity caster) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        return !state.isAir() && state.is(TEARABLE) && !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && hardness >= 0 && hardness <= MAX_HARDNESS && !state.is(BlockTagProvider.BREAK_BLACKLIST)
                && BlockUtil.destroyRespectsClaim(ANFakePlayer.getOrFakePlayer(level, caster), level, pos);
    }

    /** Tears a block out of the world into the player's hoard. Returns whether it went. */
    public static boolean tear(ServerLevel level, BlockPos pos, Player player) {
        if (space(player) <= 0 || !tearable(level, pos, player)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        level.levelEvent(2001, pos, Block.getId(state));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return add(player, state);
    }

    /** "Stone: 24/48" over the hotbar. */
    public static void report(Player player, String key, Object... args) {
        Object[] all = new Object[args.length + 2];
        System.arraycopy(args, 0, all, 0, args.length);
        all[args.length] = count(player);
        all[args.length + 1] = MAX;
        player.displayClientMessage(Component.translatable(key, all).withStyle(ChatFormatting.GRAY), true);
    }

    /** On death the hoard falls where its owner fell. */
    public static void spill(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockState state : of(player)) {
            ItemStack stack = new ItemStack(state.getBlock().asItem());
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, player.getX(), player.getY() + 0.5, player.getZ(), stack);
            }
        }
        clear(player);
    }
}
