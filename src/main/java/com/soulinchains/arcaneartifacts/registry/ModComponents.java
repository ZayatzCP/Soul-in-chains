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

    /** Souls kept in a Soul Lantern. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.soulinchains.arcaneartifacts.necro.SoulStore>> SOUL_STORE =
            REGISTER.registerComponentType("soul_store", b -> b.persistent(com.soulinchains.arcaneartifacts.necro.SoulStore.CODEC)
                    .networkSynchronized(com.soulinchains.arcaneartifacts.necro.SoulStore.STREAM_CODEC));

    /** The soul inside a Soul Vessel (ordinal of SoulType); absent = empty vessel. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VESSEL_SOUL =
            REGISTER.registerComponentType("vessel_soul", b -> b.persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /** Look, binding and cooldown of a Phylactery Amulet. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.soulinchains.arcaneartifacts.necro.Phylactery>> PHYLACTERY =
            REGISTER.registerComponentType("phylactery", b -> b.persistent(com.soulinchains.arcaneartifacts.necro.Phylactery.CODEC)
                    .networkSynchronized(com.soulinchains.arcaneartifacts.necro.Phylactery.STREAM_CODEC));

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
    /** Soul shards chipped together by the Soul Flail (10 make a soul). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SOUL_SHARDS =
            REGISTER.registerComponentType("soul_shards", b -> b.persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    /** The circle a Staff of the Five Circles is set to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DRUID_CIRCLE =
            REGISTER.registerComponentType("druid_circle", b -> b.persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    /** Rounds left in a firearm's magazine. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_ROUNDS =
            REGISTER.registerComponentType("gun_rounds", b -> b.persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /** Parts fitted into a firearm at Farraday's Workshop (barrel, cylinder, stock). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> GUN_PARTS =
            REGISTER.registerComponentType("gun_parts", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
                    .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));

    private ModComponents() {
    }
}
