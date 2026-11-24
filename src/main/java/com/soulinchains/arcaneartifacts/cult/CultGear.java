package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The numbers of the rite, in one place, so the Cultist gear (later versions) can bend them: how much a mend gives, how
 * wide the inscription window is, what a strike or a miss is worth, what a blow on the Confessor costs.
 */
public final class CultGear {
    public static final int REPAIR = 25;
    public static final float STRIKE = 5F;
    public static final float MISS = 3F;
    public static final float HURT = 2F;

    private CultGear() {
    }

    public static int repairAmount(Player warden) {
        return REPAIR;
    }

    public static int startStability(Player warden) {
        return RitualAnchorBlockEntity.MAX;
    }

    public static float strikeBonus(Player confessor, boolean closing) {
        return STRIKE;
    }

    public static float missPenalty(Player confessor) {
        return MISS;
    }

    public static float hurtPenalty(@Nullable Player confessor) {
        return HURT;
    }

    public static int windowTicks(Player confessor) {
        return RitualSealBlockEntity.WINDOW_TICKS;
    }

    /** The Book of Arrears (a later version) remembers failed rites. */
    public static boolean keepsDebt(Player player) {
        return false;
    }

    /** Both in full sets (a later version): +5%. */
    public static float pairBonus(Player confessor, @Nullable Player warden) {
        return 0F;
    }
}
