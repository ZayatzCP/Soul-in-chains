package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Raise Dead (Overseer): spends souls from the lantern and raises a servant where the spell lands. The servant is
 * chosen by the souls you carry, strongest first: void → Shadow Assassin, blazing → Burning Knight, cursed → Plague
 * Doctor, 2 drowned → Drowned Rower, 2 beast → Ghost Hound, 3 wandering → Chained Skeleton. Dampen spends only weak
 * souls; Sensitive raises a Bone Porter (4 wandering); each AOE raises one more if souls and the limit allow.
 */
public class EffectRaiseDead extends AreaEffect {
    public static final EffectRaiseDead INSTANCE = new EffectRaiseDead();
    private static final List<ServantKind> ORDER = List.of(ServantKind.ASSASSIN, ServantKind.KNIGHT, ServantKind.PLAGUE_DOCTOR,
            ServantKind.ROWER, ServantKind.HOUND, ServantKind.SKELETON);

    private EffectRaiseDead() {
        super(ArcaneArtifacts.id("glyph_raise_dead"), "Raise Dead");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (!(shooter instanceof Player player) || shooter instanceof ANFakePlayer || !Necro.requireRank(player, Necro.OVERSEER)) {
            return;
        }
        int count = 1 + (int) Math.max(0, stats.getAoeMultiplier());
        for (int i = 0; i < count; i++) {
            if (Servant.of(player.getUUID()).size() >= Necro.servantLimit(player)) {
                player.displayClientMessage(Component.translatable("arcane_artifacts.necro.servant_limit", Necro.servantLimit(player)), true);
                return;
            }
            ServantKind kind = choose(player, stats.isSensitive(), stats.hasBuff(AugmentDampen.INSTANCE));
            if (kind == null) {
                player.displayClientMessage(Component.translatable("arcane_artifacts.necro.no_souls"), true);
                return;
            }
            float reserve = Servant.reserveOf(player.getUUID()) + kind.reserve;
            if (reserve > Config.SERVANT_MAX_RESERVE.get()) {
                player.displayClientMessage(Component.translatable("arcane_artifacts.necro.no_mana_room"), true);
                return;
            }
            Vec3 at = target != null ? target.position().add(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5) : point;
            if (raise(level, player, kind, at) == null) {
                return;
            }
        }
    }

    /** The servant the player's souls can pay for, or null. */
    @Nullable
    public static ServantKind choose(Player player, boolean porter, boolean weakOnly) {
        if (porter) {
            return canPay(player, ServantKind.PORTER) ? ServantKind.PORTER : null;
        }
        for (ServantKind kind : ORDER) {
            boolean weak = kind.cost.keySet().stream().allMatch(s -> s.strength == 1);
            if ((!weakOnly || weak) && canPay(player, kind)) {
                return kind;
            }
        }
        return null;
    }

    public static boolean canPay(Player player, ServantKind kind) {
        for (Map.Entry<SoulType, Integer> e : kind.cost.entrySet()) {
            if (Souls.count(player, e.getKey()) < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** Spends the souls and raises the servant. */
    @Nullable
    public static Servant raise(ServerLevel level, Player master, ServantKind kind, Vec3 at) {
        if (!canPay(master, kind)) {
            return null;
        }
        Servant servant = ModEntities.SERVANTS.get(kind).get().create(level);
        if (servant == null) {
            return null;
        }
        for (Map.Entry<SoulType, Integer> e : kind.cost.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) {
                Souls.take(master, e.getKey());
            }
        }
        servant.moveTo(at.x, at.y, at.z, master.getYRot(), 0);
        servant.dress();
        servant.bind(master);
        level.addFreshEntity(servant);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1, at.z, 24, 0.4, 0.8, 0.4, 0.03);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.2, at.z, 12, 0.4, 0.1, 0.4, 0.01);
        level.playSound(null, servant.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.6F, 0.6F);
        return servant;
    }

    @Override
    public int getDefaultManaCost() {
        return 60;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentDampen.INSTANCE, AugmentSensitive.INSTANCE);
    }

    @Override
    protected void addDefaultAugmentLimits(Map<net.minecraft.resources.ResourceLocation, Integer> defaults) {
        super.addDefaultAugmentLimits(defaults);
        defaults.put(AugmentAOE.INSTANCE.getRegistryName(), 2);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "Raises one more servant, if souls and your limit allow.");
        map.put(AugmentDampen.INSTANCE, "Spends only weak souls (skeleton, hound, rower).");
        map.put(AugmentSensitive.INSTANCE, "Raises a Bone Porter (4 wandering souls).");
    }

    @Override
    public String getBookDescription() {
        return "Overseer rank. Spends souls from your lantern and raises a servant chained to you.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
