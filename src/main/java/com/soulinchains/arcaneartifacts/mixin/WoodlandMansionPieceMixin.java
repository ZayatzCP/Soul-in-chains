package com.soulinchains.arcaneartifacts.mixin;

import com.soulinchains.arcaneartifacts.grove.MansionLights;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The illagers of the woodland mansions keep captive Thicket Lights in cages next to them (see {@link MansionLights}). */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces$WoodlandMansionPiece")
public abstract class WoodlandMansionPieceMixin {
    @Inject(method = "handleDataMarker", at = @At("TAIL"))
    private void arcaneArtifacts$cagedLights(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box, CallbackInfo ci) {
        MansionLights.onMarker(name, pos, level, random, box);
    }
}
