package com.soulinchains.arcaneartifacts.druid;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;

/**
 * The Druid's gear: a herbalist-alchemist's outfit (see {@link RootMagic} for the events).
 * <ul>
 *     <li>Mantle (back): potions you drink last 30% longer;</li>
 *     <li>Hat (head): a keen nose, magic mushrooms and herbs around glow for you;</li>
 *     <li>Gloves (hands): mushrooms, herbs, berries and crops you gather drop more half the time;</li>
 *     <li>Boots (feet): immune to Poison and Spore Infection, farmland is never trampled.</li>
 * </ul>
 * All four ("Grand Brew"): a potion you drink is not used up 30% of the time.
 */
public class DruidGearItem extends ArcaneAccessoryItem {
    public enum Piece { MANTLE, HAT, GLOVES, BOOTS }

    public static final float POTION_LONGER = 1.3F;
    public static final float EXTRA_HARVEST = 0.5F;
    public static final float FREE_POTION = 0.3F;
    public static final int NOSE_INTERVAL = 60;
    public static final int NOSE_RADIUS = 10;

    private final Piece piece;

    public DruidGearItem(Piece piece) {
        super(new Item.Properties().rarity(Rarity.UNCOMMON), 2);
        this.piece = piece;
    }

    @Override
    protected double manaRegenBonus() {
        return 1.0;
    }

    public Piece piece() {
        return piece;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (slotContext.cosmetic() || wearer.level().isClientSide) {
            return;
        }
        if (piece == Piece.BOOTS) {
            cleanse(wearer);
        } else if (piece == Piece.HAT && wearer.tickCount % NOSE_INTERVAL == 0 && wearer instanceof Player player) {
            RootMagic.sniff(player, NOSE_RADIUS);
        }
    }

    /** The boots: an alchemist used to her own brews shrugs off poison and spores. */
    public static boolean cleanse(LivingEntity wearer) {
        boolean cleansed = false;
        if (wearer.hasEffect(MobEffects.POISON)) {
            wearer.removeEffect(MobEffects.POISON);
            cleansed = true;
        }
        if (wearer.hasEffect(ModEffects.SPORE_INFECTION)) {
            wearer.removeEffect(ModEffects.SPORE_INFECTION);
            cleansed = true;
        }
        return cleansed;
    }
}
