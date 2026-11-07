package com.soulinchains.arcaneartifacts.registry;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    /** Damage types treated as "magic" by the Wreath of Manipulation and the Arcane Adaptation effect. */
    public static final TagKey<DamageType> MAGIC_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, ArcaneArtifacts.id("magic"));
    /** Damage types the wreath never adapts to (in addition to falling and void-like damage). */
    public static final TagKey<DamageType> WREATH_IGNORED = TagKey.create(Registries.DAMAGE_TYPE, ArcaneArtifacts.id("wreath_ignored"));
    /** Damage types Parasitism never redirects (they always reach the caster). */
    public static final TagKey<DamageType> NOT_REDIRECTABLE = TagKey.create(Registries.DAMAGE_TYPE, ArcaneArtifacts.id("not_redirectable"));
    /** Non-Ars projectiles that the Mantle of Reflection may send back. Ars spell projectiles are always eligible. */
    public static final TagKey<EntityType<?>> REFLECTABLE_PROJECTILES = TagKey.create(Registries.ENTITY_TYPE, ArcaneArtifacts.id("reflectable_projectiles"));
    /** Projectiles the Mantle never reflects, even if they are Ars spell projectiles. */
    public static final TagKey<EntityType<?>> UNREFLECTABLE_PROJECTILES = TagKey.create(Registries.ENTITY_TYPE, ArcaneArtifacts.id("unreflectable_projectiles"));

    private ModTags() {
    }
}
