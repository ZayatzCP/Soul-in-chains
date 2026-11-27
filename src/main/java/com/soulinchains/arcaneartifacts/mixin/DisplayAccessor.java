package com.soulinchains.arcaneartifacts.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the stone wings turn their feathers freely (a block display's full transformation, eased on the client). */
@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setTransformation")
    void arcaneArtifacts$setTransformation(Transformation transformation);

    @Invoker("setTransformationInterpolationDuration")
    void arcaneArtifacts$setInterpolationDuration(int ticks);

    @Invoker("setTransformationInterpolationDelay")
    void arcaneArtifacts$setInterpolationDelay(int ticks);
}
