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

    /** The three pouches of the Belt of Bottomless Pouches. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.soulinchains.arcaneartifacts.belt.BeltContents>> BELT_CONTENTS =
            REGISTER.registerComponentType("belt_contents", b -> b.persistent(com.soulinchains.arcaneartifacts.belt.BeltContents.CODEC)
                    .networkSynchronized(com.soulinchains.arcaneartifacts.belt.BeltContents.STREAM_CODEC));

    /** Polarity of the Amulet of Opposites: true = repels. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> REPELLING =
            REGISTER.registerComponentType("repelling", b -> b.persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    /** Team of an Oath Scroll or a sworn suit charm (bound forever). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.soulinchains.arcaneartifacts.team.TeamBond>> TEAM_BOND =
            REGISTER.registerComponentType("team_bond", b -> b.persistent(com.soulinchains.arcaneartifacts.team.TeamBond.CODEC)
                    .networkSynchronized(com.soulinchains.arcaneartifacts.team.TeamBond.STREAM_CODEC));

    /** Magic cartridges inside a Bandolier. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CARTRIDGES =
            REGISTER.registerComponentType("cartridges", b -> b.persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    /** Rounds left in a firearm's magazine. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_ROUNDS =
            REGISTER.registerComponentType("gun_rounds", b -> b.persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    private ModComponents() {
    }
}
