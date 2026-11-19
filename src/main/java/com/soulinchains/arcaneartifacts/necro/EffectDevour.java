package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Devour (Overseer): burns the weakest soul in your lantern. It heals 3 hearts, or with Sensitive refills mana instead
 * (25 for a weak soul, 40 for a strong one, 60 cursed, 400 great or pure). Costs no mana itself.
 */
public class EffectDevour extends AreaEffect {
    public static final EffectDevour INSTANCE = new EffectDevour();
    public static final float HEAL = 6.0F;

    private EffectDevour() {
        super(ArcaneArtifacts.id("glyph_devour"), "Devour");
    }

    public static int manaOf(SoulType type) {
        return switch (type) {
            case GREAT, PURE -> 400;
            case CURSED -> 60;
            case BLAZING, VOID -> 40;
            default -> 25;
        };
    }

    /** The weakest soul the player carries, or null. */
    @Nullable
    public static SoulType weakest(Player player) {
        SoulType best = null;
        for (SoulType type : SoulType.values()) {
            if (Souls.count(player, type) > 0 && (best == null || type.strength < best.strength)) {
                best = type;
            }
        }
        return best;
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (!(shooter instanceof Player player) || !Necro.requireRank(player, Necro.OVERSEER)) {
            return;
        }
        SoulType soul = weakest(player);
        if (soul == null || !Souls.spend(player, soul)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.necro.no_souls"), true);
            return;
        }
        if (stats.isSensitive()) {
            var mana = CapabilityRegistry.getMana(player);
            if (mana != null) {
                mana.addMana(manaOf(soul));
                if (player instanceof ServerPlayer serverPlayer) {
                    mana.syncToClient(serverPlayer);
                }
            }
        } else {
            player.heal(HEAL);
        }
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.2, player.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.4F);
    }

    @Override
    public int getDefaultManaCost() {
        return 0;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentSensitive.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentSensitive.INSTANCE, "Refills mana instead of healing.");
    }

    @Override
    public String getBookDescription() {
        return "Overseer rank. Burns the weakest soul in your lantern: heals 3 hearts, or refills mana with Sensitive.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
