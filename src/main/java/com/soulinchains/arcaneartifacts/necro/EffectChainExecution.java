package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chain Execution (Chainlord): every creature within 16 blocks that is held by YOUR chains (Shackled or under the
 * Curse of Shackles you put on it), up to 8, is torn at by them: 4 + Corruption/10 damage (+2 per Amplify).
 */
public class EffectChainExecution extends AreaEffect {
    public static final EffectChainExecution INSTANCE = new EffectChainExecution();
    public static final double RANGE = 16;
    public static final int MAX_TARGETS = 8;

    private EffectChainExecution() {
        super(ArcaneArtifacts.id("glyph_chain_execution"), "Chain Execution");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (shooter instanceof Player player && !Necro.requireRank(player, Necro.CHAINLORD)) {
            return;
        }
        float corruption = shooter instanceof Player player ? Corruption.get(player) : 0F;
        float damage = 4F + corruption / 10F + 2F * (float) Math.max(0, stats.getAmpMultiplier());
        execute(level, shooter, damage);
    }

    /** Returns how many were struck. */
    public static int execute(ServerLevel level, LivingEntity caster, float damage) {
        List<LivingEntity> held = level.getEntitiesOfClass(LivingEntity.class, new AABB(caster.blockPosition()).inflate(RANGE),
                        e -> e != caster && e.isAlive() && heldBy(e, caster) && !Necro.friendly(caster, e)).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(caster))).limit(MAX_TARGETS).toList();
        for (LivingEntity victim : held) {
            victim.invulnerableTime = 0;
            victim.hurt(level.damageSources().indirectMagic(caster, caster), damage);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
        }
        if (!held.isEmpty()) {
            level.playSound(null, caster.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.5F, 0.4F);
        }
        return held.size();
    }

    public static boolean heldBy(LivingEntity victim, LivingEntity caster) {
        boolean chained = victim.hasEffect(ModEffects.SHACKLED) || victim.hasEffect(ModEffects.CURSED_CHAINS);
        return chained && victim.getPersistentData().hasUUID(EffectShackle.SHACKLED_BY)
                && victim.getPersistentData().getUUID(EffectShackle.SHACKLED_BY).equals(caster.getUUID());
    }

    @Override
    public int getDefaultManaCost() {
        return 150;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAmplify.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Two more damage to each.");
    }

    @Override
    public String getBookDescription() {
        return "Chainlord rank. Every creature within 16 blocks held by your chains (Shackle, Curse of Shackles), up to 8, takes 4 + Corruption/10 damage.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
