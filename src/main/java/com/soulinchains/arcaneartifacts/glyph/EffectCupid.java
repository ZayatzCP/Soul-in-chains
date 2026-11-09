package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.fx.Fx;
import com.soulinchains.arcaneartifacts.fx.ModTimelines;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Cupid: the target animal falls in love and looks for a partner, as if it had been fed. No augments
 * (AOE and Amplify do nothing), and it cannot share a spell with Burst.
 */
public class EffectCupid extends AreaEffect {
    public static final EffectCupid INSTANCE = new EffectCupid();

    private EffectCupid() {
        super(ArcaneArtifacts.id("glyph_cupid"), "Cupid");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (!(target instanceof Animal animal) || animal.isBaby() || animal.getAge() != 0) {
            return;
        }
        animal.setInLove(shooter instanceof Player player ? player : null);
        level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getEyeY() + 0.3, animal.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
        Fx.burst(level, Fx.timeline(context, ModTimelines.CUPID).main, animal.position().add(0, animal.getBbHeight() / 2, 0));
    }

    @Override
    protected void addDefaultInvalidCombos(Set<ResourceLocation> defaults) {
        defaults.add(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_burst"));
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
        return Set.of();
    }

    @Override
    public String getBookDescription() {
        return "The target animal falls in love, as if it had been fed its favourite food. Works on one animal at a time: AOE and Amplify do nothing, and it cannot be combined with Burst.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.CONJURATION);
    }
}
