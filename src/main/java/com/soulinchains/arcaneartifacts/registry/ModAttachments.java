package com.soulinchains.arcaneartifacts.registry;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.accessory.WreathRuntime;
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

    /** Game time of the last ring charge gain (anti-exploit rate limit). Saved with the player. */
    public static final Supplier<AttachmentType<Long>> LAST_REVERSAL_CHARGE = REGISTER.register("last_reversal_charge",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    /** Transient runtime state of the wreath (regeneration timer, cleanse tracking). Not saved. */
    public static final Supplier<AttachmentType<WreathRuntime>> WREATH_RUNTIME = REGISTER.register("wreath_runtime",
            () -> AttachmentType.builder(WreathRuntime::new).build());

    private ModAttachments() {
    }
}
