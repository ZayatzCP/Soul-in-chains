package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An effect that happens at a point: the centre of the creature that was hit, or the face of the
 * block that was hit (slightly outside it, so the point is in the air).
 */
public abstract class AreaEffect extends AbstractEffect {
    protected AreaEffect(net.minecraft.resources.ResourceLocation id, String name) {
        super(id, name);
    }

    @Override
    public void onResolve(HitResult hit, Level level, @NotNull LivingEntity shooter, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 point;
        @Nullable net.minecraft.world.entity.Entity target = null;
        if (hit instanceof EntityHitResult entityHit) {
            target = entityHit.getEntity();
            point = target.position().add(0, target.getBbHeight() / 2, 0);
        } else if (hit instanceof BlockHitResult blockHit) {
            point = Vec3.atCenterOf(blockHit.getBlockPos()).add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(1.0));
        } else {
            point = hit.getLocation();
        }
        resolveAt(server, point, target, shooter, stats, context);
    }

    protected abstract void resolveAt(ServerLevel level, Vec3 point, @Nullable net.minecraft.world.entity.Entity target,
                                      LivingEntity shooter, SpellStats stats, SpellContext context);
}
