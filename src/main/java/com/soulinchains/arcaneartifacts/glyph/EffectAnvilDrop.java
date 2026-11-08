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
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.fx.Fx;
import com.soulinchains.arcaneartifacts.fx.ModTimelines;
import com.soulinchains.arcaneartifacts.phenomena.AnvilDrop;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Anvil Drop: drops an anvil from the sky onto the target for 60 damage. AOE makes the anvil bigger and
 * turns the impact into a mace-like slam around it; any Amplify makes it a very heavy anvil (120 damage).
 * Very expensive.
 */
public class EffectAnvilDrop extends AreaEffect {
    public static final EffectAnvilDrop INSTANCE = new EffectAnvilDrop();

    private EffectAnvilDrop() {
        super(ArcaneArtifacts.id("glyph_anvil_drop"), "Anvil Drop");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        Vec3 ground = target != null ? target.position() : landing(level, point);
        int aoe = (int) Math.round(Math.max(0, stats.getAoeMultiplier()));
        boolean heavy = stats.getAmpMultiplier() > 0;
        float damage = (float) (heavy ? Config.ANVIL_HEAVY_DAMAGE.get() : Config.ANVIL_DAMAGE.get());
        double radius = aoe == 0 ? 0 : 1.5 + 1.5 * aoe;
        float size = 1.0f + 0.6f * aoe + (heavy ? 0.4f : 0f);
        var style = Fx.timeline(context, ModTimelines.ANVIL_DROP);
        Phenomena.add(new AnvilDrop(level, shooter.getUUID(), ground, damage, radius, size, style.main, style.impact));
    }

    /** The floor under a point (up to 8 blocks down). */
    private static Vec3 landing(ServerLevel level, Vec3 point) {
        var hit = level.clip(new ClipContext(point, point.subtract(0, 8, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? point : hit.getLocation();
    }

    @Override
    public int getDefaultManaCost() {
        return 350;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentAmplify.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "A bigger anvil and a slam that hits everything around (half damage).");
        map.put(AugmentAmplify.INSTANCE, "A very heavy anvil: 120 damage instead of 60.");
    }

    @Override
    public String getBookDescription() {
        return "Drops an anvil from the sky onto the target for 60 damage. Never places a block. AOE: a bigger anvil that slams the area like a mace. Amplify: a very heavy anvil, 120 damage.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ELEMENTAL_EARTH);
    }
}
