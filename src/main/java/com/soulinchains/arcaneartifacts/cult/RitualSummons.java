package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What a rite brings up, and the one reward rule that keeps it honest: only creatures a rite summoned (marked
 * {@link #RITUAL_TAG}) drop more, only the materials in {@code arcane_artifacts:ritual_loot}, once.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RitualSummons {
    public static final String RITUAL_TAG = ArcaneArtifacts.MODID + ":ritual_summoned";
    public static final TagKey<Item> RITUAL_LOOT = TagKey.create(Registries.ITEM, ArcaneArtifacts.id("ritual_loot"));

    private RitualSummons() {
    }

    /** Marks a summoned creature with its rite's loot multiplier. */
    public static void mark(LivingEntity entity, float lootMultiplier) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("loot", lootMultiplier);
        entity.getPersistentData().put(RITUAL_TAG, tag);
    }

    public static boolean summoned(LivingEntity entity) {
        return entity.getPersistentData().contains(RITUAL_TAG);
    }

    /** Rite of the Ash Witness: how many rise and how strong, by the grade. Returns how many rose. */
    public static int summonAshWitness(ServerLevel level, BlockPos at, RitualQuality tier, @Nullable Player confessor, @Nullable Player warden) {
        int count;
        AshWitness.Grade grade;
        switch (tier) {
            case BROKEN -> {
                if (level.random.nextBoolean()) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.02);
                    return 0;
                }
                count = 1;
                grade = AshWitness.Grade.FAINT;
            }
            case WEAK -> {
                count = 1;
                grade = AshWitness.Grade.COMMON;
            }
            case STEADY -> {
                count = 1;
                grade = AshWitness.Grade.EMPOWERED;
            }
            case FLAWLESS -> {
                count = 2;
                grade = AshWitness.Grade.COMMON;
            }
            default -> {
                boolean elite = level.random.nextFloat() < 0.3F;
                count = elite ? 1 : 3;
                grade = elite ? AshWitness.Grade.ELITE : AshWitness.Grade.COMMON;
            }
        }
        for (int i = 0; i < count; i++) {
            AshWitness witness = ModEntities.ASH_WITNESS.get().create(level);
            if (witness == null) {
                continue;
            }
            double angle = Math.PI * 2 * i / count;
            witness.moveTo(at.getX() + 0.5 + Math.cos(angle) * 1.5, at.getY(), at.getZ() + 0.5 + Math.sin(angle) * 1.5, level.random.nextFloat() * 360, 0);
            witness.setup(grade, confessor, warden);
            mark(witness, tier.lootMultiplier);
            level.addFreshEntity(witness);
            level.sendParticles(ParticleTypes.ASH, witness.getX(), witness.getY() + 1, witness.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        }
        return count;
    }

    /** The bonus roll: whitelisted materials only, from rite-summoned creatures only, once. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!summoned(dead) || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag tag = dead.getPersistentData().getCompound(RITUAL_TAG);
        if (tag.getBoolean("paid")) {
            return;
        }
        tag.putBoolean("paid", true);
        float extra = tag.getFloat("loot") - 1F;
        if (extra <= 0) {
            return;
        }
        List<ItemEntity> bonus = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.is(RITUAL_LOOT)) {
                continue;
            }
            float more = stack.getCount() * extra;
            int whole = (int) more + (level.random.nextFloat() < more - (int) more ? 1 : 0);
            if (whole > 0) {
                bonus.add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), stack.copyWithCount(whole)));
            }
        }
        event.getDrops().addAll(bonus);
    }
}
