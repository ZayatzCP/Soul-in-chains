package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.damage.ParasitismManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Parasitism: binds the struck creature to the caster. While the link lasts (8 s, 32 blocks), all
 * damage aimed at the caster is redirected to the host before the caster's armor and resistances
 * apply; the host's own armor and resistances apply instead. One link at a time, 20 s cooldown.
 * All rules are enforced in {@link ParasitismManager}.
 */
public class EffectParasitism extends AbstractEffect {
    public static final EffectParasitism INSTANCE = new EffectParasitism();

    private EffectParasitism() {
        super(ArcaneArtifacts.id("glyph_parasitism"), "Parasitism");
    }

    @Override
    public void onResolveEntity(EntityHitResult hit, Level level, @NotNull LivingEntity shooter, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!(level instanceof ServerLevel serverLevel) || !(hit.getEntity() instanceof LivingEntity host)) {
            return;
        }
        ParasitismManager.tryLink(serverLevel, shooter, host);
    }

    @Override
    public int getDefaultManaCost() {
        return 80;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf();
    }

    @Override
    public String getBookDescription() {
        return "Binds the struck creature to you with a parasitic link for 8 seconds. While the link holds, every source of damage aimed at you strikes the host instead - before your armor or resistances apply, and with the host's own armor and resistances applied. The link breaks if the host dies or moves more than 32 blocks away. One link at a time; 20 second cooldown. Redirected damage is never redirected again.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
