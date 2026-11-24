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

    /** What a finished rite brings up, by its scheme and grade. Returns how many rose. */
    public static int summon(ServerLevel level, BlockPos at, RiteScheme scheme, RitualQuality tier, @Nullable Player confessor,
                             @Nullable Player warden, KnifeInlay inlay, RiteScheme last) {
        if (tier == RitualQuality.BROKEN && scheme != RiteScheme.ASH_WITNESS && level.random.nextBoolean()) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.02);
            return 0;
        }
        return switch (scheme) {
            case ASH_WITNESS -> summonAshWitness(level, at, tier, confessor, warden, inlay);
            case THREE_VOICES -> {
                int count = switch (tier) {
                    case BROKEN -> 1;
                    case WEAK, STEADY -> 2;
                    case FLAWLESS -> 3;
                    case PERFECT -> 4;
                };
                RitualServant.Grade grade = tier == RitualQuality.BROKEN ? RitualServant.Grade.FAINT
                        : tier.ordinal() >= RitualQuality.STEADY.ordinal() ? RitualServant.Grade.EMPOWERED : RitualServant.Grade.COMMON;
                yield raise(level, at, ModEntities.RED_ACOLYTE.get(), count, grade, 0, tier, confessor, warden);
            }
            case RED_VEIL -> {
                int life = 1200 + 300 * Math.max(0, tier.ordinal() - 1);
                yield raise(level, at, ModEntities.BONE_GATEKEEPER.get(), 1, tier == RitualQuality.BROKEN ? RitualServant.Grade.FAINT
                        : RitualServant.Grade.COMMON, life, tier, confessor, warden);
            }
            case ABUNDANCE -> raise(level, at, ModEntities.PSALMIST.get(), 1, RitualServant.Grade.COMMON, 0, tier, confessor, warden);
            case SECOND_COMING -> {
                float beast = tier == RitualQuality.PERFECT ? 0.5F : tier == RitualQuality.FLAWLESS ? 0.25F : 0F;
                if (level.random.nextFloat() < beast) {
                    yield raise(level, at, ModEntities.VOTIVE_BEAST.get(), 1, RitualServant.Grade.COMMON, 0, tier, confessor, warden);
                }
                RiteScheme again = last == RiteScheme.SECOND_COMING ? RiteScheme.ASH_WITNESS : last;
                yield summon(level, at, again, tier, confessor, warden, inlay, RiteScheme.ASH_WITNESS);
            }
        };
    }

    /** Brings up {@code count} of a kind around the seal, marked with the rite's loot multiplier. */
    public static int raise(ServerLevel level, BlockPos at, net.minecraft.world.entity.EntityType<? extends RitualServant> type, int count,
                            RitualServant.Grade grade, int life, RitualQuality tier, @Nullable Player confessor, @Nullable Player warden) {
        int risen = 0;
        for (int i = 0; i < count; i++) {
            RitualServant servant = type.create(level);
            if (servant == null) {
                continue;
            }
            double angle = Math.PI * 2 * i / Math.max(1, count);
            servant.moveTo(at.getX() + 0.5 + Math.cos(angle) * 1.5, at.getY(), at.getZ() + 0.5 + Math.sin(angle) * 1.5, level.random.nextFloat() * 360, 0);
            servant.setup(grade, confessor, warden, life);
            mark(servant, tier.lootMultiplier);
            level.addFreshEntity(servant);
            level.sendParticles(ParticleTypes.ASH, servant.getX(), servant.getY() + 1, servant.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
            risen++;
        }
        return risen;
    }

    /** Rite of the Ash Witness: how many rise and how strong, by the grade. Returns how many rose. */
    public static int summonAshWitness(ServerLevel level, BlockPos at, RitualQuality tier, @Nullable Player confessor, @Nullable Player warden,
                                       KnifeInlay inlay) {
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
                // the red-glass inlay sometimes calls a third
                count = inlay == KnifeInlay.RED_GLASS && level.random.nextFloat() < 0.15F ? 3 : 2;
                grade = AshWitness.Grade.COMMON;
            }
            default -> {
                boolean elite = level.random.nextFloat() < (inlay == KnifeInlay.RED_GLASS ? 0.5F : 0.3F);
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
        // a Psalmist singing nearby adds to it
        if (!level.getEntitiesOfClass(Psalmist.class, dead.getBoundingBox().inflate(Psalmist.CHOIR_RANGE), p -> p != dead && p.isAlive()).isEmpty()) {
            extra += Psalmist.LOOT_BONUS;
        }
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
