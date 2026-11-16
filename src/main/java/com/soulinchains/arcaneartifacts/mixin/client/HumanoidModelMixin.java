package com.soulinchains.arcaneartifacts.mixin.client;

import com.soulinchains.arcaneartifacts.pseudo.MagicGunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Pseudomage's gun is aimed straight along the gaze: the arm holding it points where the head looks (the
 * crossbow pose turns arms inwards, so two guns would cross).
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow
    @Final
    public ModelPart head;
    @Shadow
    @Final
    public ModelPart rightArm;
    @Shadow
    @Final
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void arcane_artifacts$aimGuns(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                          float netHeadYaw, float headPitch, CallbackInfo ci) {
        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        ItemStack inRight = rightHanded ? entity.getMainHandItem() : entity.getOffhandItem();
        ItemStack inLeft = rightHanded ? entity.getOffhandItem() : entity.getMainHandItem();
        if (inRight.getItem() instanceof MagicGunItem) {
            rightArm.xRot = head.xRot - (float) Math.PI / 2F;
            rightArm.yRot = head.yRot;
            rightArm.zRot = 0F;
        }
        if (inLeft.getItem() instanceof MagicGunItem) {
            leftArm.xRot = head.xRot - (float) Math.PI / 2F;
            leftArm.yRot = head.yRot;
            leftArm.zRot = 0F;
        }
    }
}
