package com.soulinchains.arcaneartifacts.druid;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;

/**
 * The Druid's gear (Root Magic). Each piece works while the wearer stands on living ground:
 * <ul>
 *     <li>Mantle (back): Regeneration I;</li>
 *     <li>Hat (head): photosynthesis, a little food in daylight under the open sky;</li>
 *     <li>Gloves (hands): crops and saplings around grow faster (anywhere);</li>
 *     <li>Boots (feet): +20% speed, and farmland is never trampled (anywhere).</li>
 * </ul>
 * All four: sneaking on living ground roots the druid in place, Resistance II and no knockback.
 */
public class DruidGearItem extends ArcaneAccessoryItem {
    public enum Piece { MANTLE, HAT, GLOVES, BOOTS }

    public static final ResourceLocation BOOTS_SPEED = ArcaneArtifacts.id("druid_boots_speed");
    public static final ResourceLocation ROOTED_KNOCKBACK = ArcaneArtifacts.id("druid_rooted");
    public static final double SPEED = 0.20;
    public static final int PHOTOSYNTHESIS_TICKS = 200;

    private final Piece piece;

    public DruidGearItem(Piece piece) {
        super(new Item.Properties().rarity(Rarity.UNCOMMON), 2);
        this.piece = piece;
    }

    public Piece piece() {
        return piece;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (slotContext.cosmetic() || !(wearer.level() instanceof ServerLevel level)) {
            return;
        }
        boolean living = RootMagic.onLivingGround(wearer);
        switch (piece) {
            case MANTLE -> {
                if (living && wearer.tickCount % 40 == 0) {
                    wearer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                }
            }
            case HAT -> {
                if (wearer.tickCount % PHOTOSYNTHESIS_TICKS == 0) {
                    photosynthesis(wearer);
                }
            }
            case GLOVES -> {
                if (wearer.tickCount % 20 == 0) {
                    RootMagic.tendGarden(level, wearer.blockPosition(), 4, 6);
                }
            }
            case BOOTS -> {
                if (wearer.tickCount % 10 == 0) {
                    AttributeInstance speed = wearer.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speed != null) {
                        if (living && !speed.hasModifier(BOOTS_SPEED)) {
                            speed.addTransientModifier(new AttributeModifier(BOOTS_SPEED, SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                        } else if (!living) {
                            speed.removeModifier(BOOTS_SPEED);
                        }
                    }
                    root(wearer, living);
                }
            }
        }
    }

    /** The hat feeds a player in daylight under the open sky: half a drumstick and a little saturation. */
    public static boolean photosynthesis(LivingEntity wearer) {
        if (wearer instanceof Player player && wearer.level().isDay() && wearer.level().canSeeSky(wearer.blockPosition().above())
                && RootMagic.onLivingGround(wearer)) {
            player.getFoodData().eat(1, 0.5F);
            return true;
        }
        return false;
    }

    /** The full set: sneaking on living ground roots the druid (Resistance II, no knockback). */
    public static boolean root(LivingEntity wearer, boolean living) {
        AttributeInstance knockback = wearer.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        boolean rooted = living && wearer.isShiftKeyDown() && RootMagic.fullSet(wearer);
        if (rooted) {
            wearer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, true, false, true));
            if (knockback != null && !knockback.hasModifier(ROOTED_KNOCKBACK)) {
                knockback.addTransientModifier(new AttributeModifier(ROOTED_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (knockback != null) {
            knockback.removeModifier(ROOTED_KNOCKBACK);
        }
        return rooted;
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);
        if (piece == Piece.BOOTS) {
            LivingEntity wearer = slotContext.entity();
            AttributeInstance speed = wearer.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(BOOTS_SPEED);
            }
            AttributeInstance knockback = wearer.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null) {
                knockback.removeModifier(ROOTED_KNOCKBACK);
            }
        }
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        return LinkedHashMultimap.create();
    }
}
