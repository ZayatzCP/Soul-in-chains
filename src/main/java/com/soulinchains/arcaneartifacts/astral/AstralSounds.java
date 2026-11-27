package com.soulinchains.arcaneartifacts.astral;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The sounds of the Astral (made by {@code tools/generate_astral.py}): a low hum always in the air, rare distant strikes of
 * metal, and a silence that stands in for music.
 */
public final class AstralSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, ArcaneArtifacts.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HUM = sound("astral.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> METAL = sound("astral.metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENCE = sound("astral.silence");

    private AstralSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ArcaneArtifacts.MODID, name)));
    }
}
