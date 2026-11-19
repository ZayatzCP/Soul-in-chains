package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.jetbrains.annotations.Nullable;

/**
 * The eight kinds of souls. Which mob gives which soul is decided by entity type tags
 * ({@code arcane_artifacts:souls/<id>}), so any mod's mobs can be added with a data pack. An untagged hostile mob gives
 * a wandering soul; an untagged peaceful one gives none.
 */
public enum SoulType {
    WANDERING("wandering", 1, 0xA9C7FF),
    BEAST("beast", 1, 0xE0B070),
    DROWNED("drowned", 1, 0x3FB8C8),
    BLAZING("blazing", 2, 0xFF7A1E),
    VOID("void", 2, 0xB060FF),
    CURSED("cursed", 2, 0x5ADB5A),
    GREAT("great", 3, 0xFFE070),
    PURE("pure", 3, 0xFFFFFF);

    /** Checked in this order: the first tag a mob is in wins. */
    private static final SoulType[] PRIORITY = {GREAT, PURE, CURSED, VOID, BLAZING, DROWNED, BEAST, WANDERING};

    public final String id;
    /** Soul strength 1-3; also the "level" of undead for the Bone Crown. */
    public final int strength;
    public final int color;
    public final TagKey<EntityType<?>> tag;

    SoulType(String id, int strength, int color) {
        this.id = id;
        this.strength = strength;
        this.color = color;
        this.tag = TagKey.create(Registries.ENTITY_TYPE, ArcaneArtifacts.id("souls/" + id));
    }

    /** The soul a creature leaves, or null if it has none. */
    @Nullable
    public static SoulType of(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        for (SoulType soul : PRIORITY) {
            if (type.is(soul.tag)) {
                return soul;
            }
        }
        return entity instanceof Enemy ? WANDERING : null;
    }

    /** One step up (Reap with Amplify): a strength-1 soul becomes cursed; stronger souls stay what they are. */
    public SoulType upgraded() {
        return strength == 1 ? CURSED : this;
    }

    public String translationKey() {
        return "arcane_artifacts.soul." + id;
    }

    public static SoulType byOrdinal(int i) {
        SoulType[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, i))];
    }
}
