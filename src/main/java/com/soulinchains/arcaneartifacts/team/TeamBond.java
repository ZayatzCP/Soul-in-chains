package com.soulinchains.arcaneartifacts.team;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** A sworn company: its id and its name (the name of the Oath Scroll that founded it). */
public record TeamBond(UUID team, String name) {
    public static final Codec<TeamBond> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("team").forGetter(TeamBond::team),
            Codec.STRING.fieldOf("name").forGetter(TeamBond::name)
    ).apply(i, TeamBond::new));
    public static final StreamCodec<ByteBuf, TeamBond> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TeamBond::team,
            ByteBufCodecs.STRING_UTF8, TeamBond::name,
            TeamBond::new);

    public static @Nullable TeamBond of(ItemStack stack) {
        return stack.get(ModComponents.TEAM_BOND);
    }
}
