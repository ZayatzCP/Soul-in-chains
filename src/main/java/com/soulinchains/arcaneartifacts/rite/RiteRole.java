package com.soulinchains.arcaneartifacts.rite;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * What each participant does while a rite runs. The Reader reads the formula (holds the Book of Sigils by the Heart), the
 * Tracer retraces fading sigils with chalk, the Keeper relights candles, the Sacrificer brings offerings when the Heart asks,
 * the Guard is free to move and fight. With few participants each one takes several.
 */
public enum RiteRole {
    READER, TRACER, KEEPER, SACRIFICER, GUARD;

    public Component displayName() {
        return Component.translatable("arcane_artifacts.rite.role." + name().toLowerCase(java.util.Locale.ROOT));
    }

    /** Roles for {@code n} participants, in joining order. */
    public static List<EnumSet<RiteRole>> assign(int n) {
        List<EnumSet<RiteRole>> roles = new ArrayList<>();
        switch (n) {
            case 0 -> {
            }
            case 1 -> roles.add(EnumSet.allOf(RiteRole.class));
            case 2 -> {
                roles.add(EnumSet.of(READER, SACRIFICER));
                roles.add(EnumSet.of(TRACER, KEEPER, GUARD));
            }
            case 3 -> {
                roles.add(EnumSet.of(READER, SACRIFICER));
                roles.add(EnumSet.of(TRACER));
                roles.add(EnumSet.of(KEEPER, GUARD));
            }
            default -> {
                roles.add(EnumSet.of(READER));
                roles.add(EnumSet.of(TRACER));
                roles.add(EnumSet.of(KEEPER));
                roles.add(EnumSet.of(SACRIFICER, GUARD));
                for (int i = 4; i < n; i++) {
                    roles.add(EnumSet.of(GUARD));
                }
            }
        }
        return roles;
    }
}
