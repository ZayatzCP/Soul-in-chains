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

    /** Rank of the Shackled class earned by ritual (1 Novice, 2 Overseer, 3 Chainlord); kept through death. */
    public static final Supplier<AttachmentType<Integer>> NECRO_RANK = REGISTER.register("necro_rank",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    public static final Supplier<AttachmentType<Float>> CORRUPTION = REGISTER.register("corruption",
            () -> AttachmentType.builder(() -> 0F).serialize(Codec.FLOAT).copyOnDeath().build());

    /** Where a player ascended into the Astral from; the Astral Gate sends them back there. */
    public static final Supplier<AttachmentType<com.soulinchains.arcaneartifacts.astral.Astral.Origin>> ASTRAL_ORIGIN = REGISTER.register("astral_origin",
            () -> AttachmentType.builder(() -> com.soulinchains.arcaneartifacts.astral.Astral.Origin.NONE)
                    .serialize(com.soulinchains.arcaneartifacts.astral.Astral.Origin.CODEC).copyOnDeath().build());

    /** The Stone Hoard: real blocks torn from the ground, circling the player (1.43). */
    public static final Supplier<AttachmentType<java.util.List<net.minecraft.world.level.block.state.BlockState>>> STONE_HOARD = REGISTER.register("stone_hoard",
            () -> AttachmentType.<java.util.List<net.minecraft.world.level.block.state.BlockState>>builder(() -> new java.util.ArrayList<>())
                    .serialize(com.soulinchains.arcaneartifacts.geo.StoneHoard.CODEC).build());

    private ModAttachments() {
    }
}
