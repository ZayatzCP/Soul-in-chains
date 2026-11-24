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
 * The Druid's gear, the "Roots" set (see {@link RootMagic} for the events). Living ground is grass, dirt, moss and
 * leaves ({@link RootMagic#onLivingGround}).
 * <ul>
 *     <li>Mantle (back): on living ground, +2 mana a second and Regeneration I; potions last 30% longer.</li>
 *     <li>Hat (head): Root Magic spells (Entangle, Grove, Bee Swarm, the druid staves) cost 20% less and hold one Extend
 *     Time more; magic mushrooms and herbs around glow for you.</li>
 *     <li>Gloves (hands): Root Magic spells get one Amplify; your blows hit entangled creatures twice as hard; herbs
 *     you gather sometimes drop twice.</li>
 *     <li>Boots (feet): immune to Poison and Spore Infection, faster on living ground, farmland is never trampled.</li>
 * </ul>
 * All four, "Roots": a killing blow on living ground roots you instead - 6 hearts back and 3 seconds of stone skin,
 * once every 5 minutes; a potion you drink is not used up 30% of the time.
 */
public class DruidGearItem extends ArcaneAccessoryItem {
    public enum Piece { MANTLE, HAT, GLOVES, BOOTS }

    public static final float POTION_LONGER = 1.3F;
    public static final float EXTRA_HARVEST = 0.5F;
    public static final float FREE_POTION = 0.3F;
    public static final int NOSE_INTERVAL = 60;
    public static final int NOSE_RADIUS = 10;
    public static final float MANTLE_MANA_PER_SECOND = 2F;
    public static final float HAT_DISCOUNT = 0.2F;
    public static final float GLOVES_ENTANGLED_BONUS = 2.0F;
    public static final int ROOTS_COOLDOWN_TICKS = 6000;
    public static final float ROOTS_HEAL = 12F;

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
            if (wearer.tickCount % 10 == 0 && RootMagic.onLivingGround(wearer)) {
                wearer.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, true, false, true));
            }
        } else if (piece == Piece.MANTLE && wearer.tickCount % 20 == 0 && wearer instanceof Player player && RootMagic.onLivingGround(player)) {
            RootMagic.drawFromRoots(player);
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
