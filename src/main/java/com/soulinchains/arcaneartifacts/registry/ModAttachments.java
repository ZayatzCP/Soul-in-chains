package com.soulinchains.arcaneartifacts.registry;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.research.PlayerResearch;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArcaneArtifacts.MODID);

    /**
     * Game time (overworld clock) until which the entity cannot create a new parasitic link.
     * Serialized and kept through death so that relogging or dying cannot reset the cooldown.
     */
    public static final Supplier<AttachmentType<Long>> PARASITISM_COOLDOWN_UNTIL = REGISTER.register("parasitism_cooldown_until",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath().build());

    /** Research progress of a player: saved with the world, kept through death, mirrored to the client. */
    public static final Supplier<AttachmentType<PlayerResearch>> RESEARCH = REGISTER.register("research",
            () -> AttachmentType.builder(PlayerResearch::new).serialize(PlayerResearch.CODEC).copyOnDeath().build());

    private ModAttachments() {
    }
}
