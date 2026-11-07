package com.soulinchains.arcaneartifacts.research;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Progress of one research for one player, in order.
 * <ol>
 *     <li>UNKNOWN - prerequisites not met, shown as an unreadable page;</li>
 *     <li>DISCOVERED - prerequisites met, the subject can be observed at a Research Table;</li>
 *     <li>INVESTIGATING - the subject was observed, experiments are being recorded;</li>
 *     <li>PRINCIPLE - every experiment succeeded, notes were awarded;</li>
 *     <li>UNLOCKED - the principle was formulated at the table, the glyph recipe is available;</li>
 *     <li>RECORDED - every unlocked glyph is known by the player (written into the spell book).</li>
 * </ol>
 */
public enum ResearchState implements StringRepresentable {
    UNKNOWN, DISCOVERED, INVESTIGATING, PRINCIPLE, UNLOCKED, RECORDED;

    public static final Codec<ResearchState> CODEC = StringRepresentable.fromEnum(ResearchState::values);

    public boolean atLeast(ResearchState other) {
        return ordinal() >= other.ordinal();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String translationKey() {
        return "arcane_artifacts.research_state." + getSerializedName();
    }
}
