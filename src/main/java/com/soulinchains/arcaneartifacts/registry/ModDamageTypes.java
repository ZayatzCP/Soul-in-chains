package com.soulinchains.arcaneartifacts.registry;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ModDamageTypes {
    /**
     * Used by Parasitism when the host is immune to the original damage type (e.g. redirected fire
     * damage onto a fire-immune host). Not armor-bypassing: the host's armor and resistances apply.
     */
    public static final ResourceKey<DamageType> PARASITIC_TRANSFER = ResourceKey.create(Registries.DAMAGE_TYPE, ArcaneArtifacts.id("parasitic_transfer"));

    public static DamageSource parasiticTransfer(Level level, @Nullable Entity direct, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PARASITIC_TRANSFER), direct, attacker);
    }

    private ModDamageTypes() {
    }
}
