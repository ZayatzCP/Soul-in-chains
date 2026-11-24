package com.soulinchains.arcaneartifacts.cult;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * A Cultist's lasting state (player attachment): the role sworn in the Pairing rite, the partner, the rank, how many steady
 * rites (60%+) the pair has finished, and the "debt" of failed rites the Book of Arrears turns into a head start.
 */
public record CultData(CultRole role, Optional<UUID> partner, int rank, int steady, int debt) {
    public static final CultData EMPTY = new CultData(CultRole.NONE, Optional.empty(), 0, 0, 0);
    public static final Codec<CultData> CODEC = RecordCodecBuilder.create(i -> i.group(
            CultRole.CODEC.optionalFieldOf("role", CultRole.NONE).forGetter(CultData::role),
            UUIDUtil.CODEC.optionalFieldOf("partner").forGetter(CultData::partner),
            Codec.INT.optionalFieldOf("rank", 0).forGetter(CultData::rank),
            Codec.INT.optionalFieldOf("steady", 0).forGetter(CultData::steady),
            Codec.INT.optionalFieldOf("debt", 0).forGetter(CultData::debt)
    ).apply(i, CultData::new));

    public boolean paired() {
        return partner.isPresent() && role != CultRole.NONE;
    }

    public CultData pairedWith(CultRole newRole, UUID with) {
        return new CultData(newRole, Optional.of(with), Math.max(rank, 1), steady, debt);
    }

    public CultData unpaired() {
        return new CultData(CultRole.NONE, Optional.empty(), rank, steady, debt);
    }

    public CultData withSteady(int value) {
        return new CultData(role, partner, rank, value, debt);
    }

    public CultData withDebt(int value) {
        return new CultData(role, partner, rank, steady, value);
    }
}
