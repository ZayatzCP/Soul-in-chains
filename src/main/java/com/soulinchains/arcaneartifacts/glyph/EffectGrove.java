package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Grove (Root Magic): everything that grows around the point is fed as if by bone meal, once per cast
 * (+1 per Amplify); AOE widens the patch. A young animal hit by it grows up a good deal.
 */
public class EffectGrove extends AreaEffect {
    public static final EffectGrove INSTANCE = new EffectGrove();

    private EffectGrove() {
        super(ArcaneArtifacts.id("glyph_grove"), "Grove");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        int times = 1 + Math.max(0, (int) Math.round(stats.getAmpMultiplier()));
        if (target instanceof AgeableMob mob && mob.isBaby()) {
            mob.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-mob.getAge()) * times, true);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 10, 0.4, 0.3, 0.4, 0);
            return;
        }
        int radius = Math.max(0, (int) Math.round(stats.getAoeMultiplier()));
        grow(level, BlockPos.containing(point).below(), radius, times);
    }

    /** Feeds every growing block within the radius (a 3-high slab) `times` times; returns how many grew. */
    public static int grow(ServerLevel level, BlockPos centre, int radius, int times) {
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -1, -radius), centre.offset(radius, 1, radius))) {
            for (int i = 0; i < times; i++) {
                ItemStack meal = new ItemStack(Items.BONE_MEAL);
                if (BoneMealItem.growCrop(meal, level, pos.immutable())) {
                    grown++;
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0);
                }
            }
        }
        return grown;
    }

    @Override
    public int getDefaultManaCost() {
        return 40;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAmplify.INSTANCE, AugmentAOE.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Feeds the plants one more time.");
        map.put(AugmentAOE.INSTANCE, "A wider patch grows.");
    }

    @Override
    public String getBookDescription() {
        return "Everything that grows around the point is fed as if with bone meal; a young animal grows up.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ELEMENTAL_EARTH);
    }
}
