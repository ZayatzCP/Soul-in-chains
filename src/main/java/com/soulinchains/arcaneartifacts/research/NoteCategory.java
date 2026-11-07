package com.soulinchains.arcaneartifacts.research;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Research note categories. Notes confirm knowledge in one field; they are never spent. */
public enum NoteCategory implements StringRepresentable {
    MATERIAL("material", 0xC9A86E),
    BIOLOGICAL("biological", 0x6FBF5A),
    ARCANE("arcane", 0xB44CF0),
    SPATIAL("spatial", 0x2FD6E0);

    public static final Codec<NoteCategory> CODEC = StringRepresentable.fromEnum(NoteCategory::values);

    private final String name;
    public final int color;

    NoteCategory(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "arcane_artifacts.notes." + name;
    }
}
