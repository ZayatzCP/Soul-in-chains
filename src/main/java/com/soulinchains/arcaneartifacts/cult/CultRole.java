package com.soulinchains.arcaneartifacts.cult;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The two halves of a Cultist pair. */
public enum CultRole implements StringRepresentable {
    NONE("none"), WARDEN("warden"), CONFESSOR("confessor");

    public static final Codec<CultRole> CODEC = StringRepresentable.fromEnum(CultRole::values);
    public final String id;

    CultRole(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
