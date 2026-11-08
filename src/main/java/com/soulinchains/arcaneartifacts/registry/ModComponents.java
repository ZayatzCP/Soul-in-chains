package com.soulinchains.arcaneartifacts.registry;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.accessory.RushState;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(ArcaneArtifacts.MODID);

    /** Battle Rush timings of the Wreath of Manipulation. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RushState>> RUSH_STATE =
            REGISTER.registerComponentType("rush_state", b -> b.persistent(RushState.CODEC).networkSynchronized(RushState.STREAM_CODEC));

    private ModComponents() {
    }
}
