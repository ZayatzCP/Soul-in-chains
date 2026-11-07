package com.soulinchains.arcaneartifacts.registry;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.accessory.WreathData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(ArcaneArtifacts.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WreathData>> WREATH_DATA =
            REGISTER.registerComponentType("wreath_data", b -> b.persistent(WreathData.CODEC).networkSynchronized(WreathData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> REVERSAL_CHARGE =
            REGISTER.registerComponentType("reversal_charge", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModComponents() {
    }
}
