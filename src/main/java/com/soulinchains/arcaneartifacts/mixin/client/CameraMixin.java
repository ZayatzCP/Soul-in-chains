package com.soulinchains.arcaneartifacts.mixin.client;

import com.soulinchains.arcaneartifacts.client.catto.CattoCinematic;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The conversation camera: after the game has placed the camera as usual, a talk with Catto moves it over the player's
 * right shoulder and turns it to Catto, blending in and out (see {@link CattoCinematic}).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private Vec3 position;
    @Shadow
    private float xRot;
    @Shadow
    private float yRot;

    @Final
    @Shadow
    private Quaternionf rotation;
    @Final
    @Shadow
    private Vector3f forwards;
    @Final
    @Shadow
    private Vector3f up;
    @Final
    @Shadow
    private Vector3f left;

    @Inject(method = "setup", at = @At("TAIL"))
    private void arcaneArtifacts$cattoCinematic(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        CattoCinematic.Shot shot = CattoCinematic.shot(entity, position, yRot, xRot, partialTick);
        if (shot != null) {
            position = shot.position();
            yRot = shot.yaw();
            xRot = shot.pitch();
            rotation.rotationYXZ((float) Math.PI - yRot * ((float) Math.PI / 180.0F), -xRot * ((float) Math.PI / 180.0F), 0.0F);
            forwards.set(0.0F, 0.0F, -1.0F).rotate(rotation);
            up.set(0.0F, 1.0F, 0.0F).rotate(rotation);
            left.set(-1.0F, 0.0F, 0.0F).rotate(rotation);
        }
    }
}
