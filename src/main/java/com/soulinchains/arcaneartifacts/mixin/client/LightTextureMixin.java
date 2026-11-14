package com.soulinchains.arcaneartifacts.mixin.client;

import com.soulinchains.arcaneartifacts.client.CaveBrightness;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lifts the darkness in the Mushroom Caves on the client only (see {@link CaveBrightness}). */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Inject(method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", at = @At("RETURN"), cancellable = true, require = 0)
    private static void arcaneArtifacts$caveGlow(DimensionType dimensionType, int lightLevel, CallbackInfoReturnable<Float> cir) {
        float boost = CaveBrightness.boost();
        if (boost > 0F) {
            cir.setReturnValue(Mth.lerp(boost, cir.getReturnValueF(), 1.0F));
        }
    }
}
