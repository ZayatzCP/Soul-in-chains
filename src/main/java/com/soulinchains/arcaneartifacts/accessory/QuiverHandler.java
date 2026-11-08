package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.capability.ManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import java.util.Optional;

/** Spell Quiver: enchants arrows when they are shot and releases the spell where they hit. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class QuiverHandler {
    public static final String TAG = ArcaneArtifacts.MODID + ":quiver_spell";

    private QuiverHandler() {
    }

    @SubscribeEvent
    public static void onArrowShot(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof Player player) || arrow.getPersistentData().contains(TAG)) {
            return;
        }
        Optional<ItemStack> quiver = CurioHelper.findEquipped(player, ModItems.SPELL_QUIVER.get());
        if (quiver.isEmpty()) {
            return;
        }
        Spell stored = SpellQuiverItem.storedSpell(quiver.get());
        if (!SpellQuiverItem.hasEffect(stored) || ResearchService.firstLockedGlyph(player, stored) != null) {
            return;
        }
        Spell spell = SpellQuiverItem.arrowSpell(stored);
        if (!player.isCreative()) {
            ManaCap mana = CapabilityRegistry.getMana(player);
            int cost = spell.getCost();
            if (mana == null || mana.getCurrentMana() < cost) {
                return;
            }
            mana.removeMana(cost);
            if (player instanceof ServerPlayer serverPlayer) {
                mana.syncToClient(serverPlayer);
            }
        }
        arrow.getPersistentData().putBoolean(TAG, true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onArrowHit(ProjectileImpactEvent event) {
        if (event.isCanceled() || !(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)
                || !arrow.getPersistentData().getBoolean(TAG) || event.getRayTraceResult().getType() == HitResult.Type.MISS) {
            return;
        }
        if (!(arrow.getOwner() instanceof Player player)) {
            return;
        }
        arrow.getPersistentData().remove(TAG);
        Optional<ItemStack> quiver = CurioHelper.findEquipped(player, ModItems.SPELL_QUIVER.get());
        Spell stored = quiver.map(SpellQuiverItem::storedSpell).orElse(new Spell());
        if (!SpellQuiverItem.hasEffect(stored)) {
            return;
        }
        Spell spell = SpellQuiverItem.arrowSpell(stored);
        SpellContext context = new SpellContext(level, spell, player, LivingCaster.from(player), quiver.get());
        new SpellResolver(context).onResolveEffect(level, event.getRayTraceResult());
    }
}
