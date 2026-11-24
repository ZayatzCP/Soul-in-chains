package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.accessory.ArcaneSets;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The numbers of the rite, in one place, bent by the Cultist gear:
 * <ul>
 *     <li>Seal-Wright's Gauntlets: mends 1.5x; the Belt of Anchors adds +35 to a shield mend while it has charges.</li>
 *     <li>Face of Dogma: the window is 6 ticks wider. Hands of the Inscriber: a strike is worth +1 more, a miss costs 1.</li>
 *     <li>Voice of the Rite (full Confessor set): +2 per strike up to +10 a rite, the close counts twice.</li>
 *     <li>Mantle of Communion: a blow on the Confessor costs half. Book of Arrears: failed rites become a head start.</li>
 *     <li>Seal-Keeper's Helm: anchors near the Warden take 40% less from the Torn Acolytes.</li>
 *     <li>Cuirass of the Outer Circle: the Interception takes 40% instead of 30%.</li>
 *     <li>Both in full sets: +5%.</li>
 * </ul>
 */
public final class CultGear {
    public static final int REPAIR = 25;
    public static final float STRIKE = 5F;
    public static final float MISS = 3F;
    public static final float HURT = 2F;
    public static final int BELT_CHARGE_REPAIR = 35;
    public static final int BELT_CHARGES = 4;
    public static final int FACE_WINDOW_BONUS = 6;
    public static final float VOICE_STRIKE = 2F;
    public static final float VOICE_MAX = 10F;
    public static final float PAIR_BONUS = 5F;
    public static final float HELM_ANCHOR = 0.6F;
    public static final double HELM_RANGE = 8;
    public static final float CUIRASS_SHARE = 0.4F;

    private CultGear() {
    }

    public static boolean wears(@Nullable LivingEntity entity, Item item) {
        return entity != null && ArcaneSets.has(entity, item);
    }

    public static boolean wardenSet(@Nullable LivingEntity entity) {
        return entity != null && ArcaneSets.Set.CIRCLE_KEEPER.complete(entity);
    }

    public static boolean confessorSet(@Nullable LivingEntity entity) {
        return entity != null && ArcaneSets.Set.VOICE_OF_RITE.complete(entity);
    }

    public static int repairAmount(Player warden) {
        return wears(warden, ModItems.SEAL_GAUNTLETS.get()) ? Math.round(REPAIR * 1.5F) : REPAIR;
    }

    public static int hammerRepair(Player warden) {
        return CultTools.HAMMER_REPAIR + (wears(warden, ModItems.SEAL_GAUNTLETS.get()) ? 15 : 0);
    }

    public static int startStability(Player warden) {
        return RitualAnchorBlockEntity.MAX;
    }

    public static float strikeBonus(Player confessor, boolean closing) {
        float bonus = STRIKE + (wears(confessor, ModItems.INSCRIBER_HANDS.get()) ? 1F : 0F);
        return closing && confessorSet(confessor) ? bonus * 2F : bonus;
    }

    /** The Voice of the Rite's extra for a strike, given how much it already gave in this rite. */
    public static float voiceBonus(Player confessor, float alreadyGiven) {
        return confessorSet(confessor) ? Math.max(0F, Math.min(VOICE_STRIKE, VOICE_MAX - alreadyGiven)) : 0F;
    }

    public static float missPenalty(Player confessor) {
        return wears(confessor, ModItems.INSCRIBER_HANDS.get()) ? 1F : MISS;
    }

    public static float hurtPenalty(@Nullable Player confessor) {
        return wears(confessor, ModItems.COMMUNION_MANTLE.get()) ? HURT / 2F : HURT;
    }

    public static int windowTicks(Player confessor) {
        return RitualSealBlockEntity.WINDOW_TICKS + (wears(confessor, ModItems.FACE_OF_DOGMA.get()) ? FACE_WINDOW_BONUS : 0);
    }

    /** The Book of Arrears remembers failed rites. */
    public static boolean keepsDebt(Player player) {
        return wears(player, ModItems.BOOK_OF_ARREARS.get());
    }

    /** Both in full sets: +5%. */
    public static float pairBonus(Player confessor, @Nullable Player warden) {
        return confessorSet(confessor) && wardenSet(warden) ? PAIR_BONUS : 0F;
    }

    /** The helm: anchors near its wearer take 40% less. */
    public static int anchorDamage(int base, @Nullable Player warden, Vec3 anchor) {
        if (warden != null && wears(warden, ModItems.SEAL_KEEPER_HELM.get()) && warden.position().distanceTo(anchor) <= HELM_RANGE) {
            return Math.round(base * HELM_ANCHOR);
        }
        return base;
    }

    /** The cuirass: the Interception takes 40% of a creature's blow. */
    public static float interceptShare(Player warden, float base) {
        return wears(warden, ModItems.OUTER_CIRCLE_CUIRASS.get()) && base >= CultTools.INTERCEPT_SHARE ? CUIRASS_SHARE : base;
    }
}
