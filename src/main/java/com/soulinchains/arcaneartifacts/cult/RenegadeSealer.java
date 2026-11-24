package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Renegade Sealer (elite, the Archive): a printer of seals who broke his oath. Goes for any lit anchor within 16 blocks
 * and strikes it (15 stability every 1.5 s); near a Holding Seal it breaks the channel. Turns on whoever hurts it.
 */
public class RenegadeSealer extends TornAcolyte {
    public static final int SEARCH = 16;
    @Nullable
    private RitualAnchorBlockEntity cached;
    private int nextSearch;

    public RenegadeSealer(EntityType<? extends RenegadeSealer> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected boolean fades() {
        return false;
    }

    /** The nearest lit anchor within 16 blocks, whoever's circle it belongs to (searched once a second). */
    @Override
    @Nullable
    public RitualAnchorBlockEntity nearestAnchor() {
        if (cached != null && (cached.isRemoved() || !cached.active())) {
            cached = null;
        }
        if (tickCount < nextSearch && cached != null) {
            return cached;
        }
        nextSearch = tickCount + 20;
        RitualAnchorBlockEntity best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos here = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(here.offset(-SEARCH, -3, -SEARCH), here.offset(SEARCH, 3, SEARCH))) {
            if (level().getBlockEntity(p) instanceof RitualAnchorBlockEntity anchor && anchor.active()) {
                double d = distanceToSqr(Vec3.atCenterOf(p));
                if (d < bestD) {
                    bestD = d;
                    best = anchor;
                }
            }
        }
        cached = best;
        return best;
    }
}
