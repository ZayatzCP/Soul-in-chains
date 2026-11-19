package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Awakening (Ritual Brazier): a Novice of the Shackled offers ten souls from the lantern and becomes an Overseer:
 * servants, Raise Dead, Devour and Bond open up.
 */
public class AwakeningRitual extends AbstractRitual {
    public static final String ID = "ritual_awakening";
    public static final int SOULS = 10;

    @Override
    public ResourceLocation getRegistryName() {
        return ArcaneArtifacts.id(ID);
    }

    @Override
    public String getLangName() {
        return "Awakening";
    }

    @Override
    public String getLangDescription() {
        return "A Novice of the Shackled offers ten souls from the Soul Lantern and becomes an Overseer: servants, Raise Dead, Devour and Bond open up.";
    }

    @Override
    public int getSourceCost() {
        return 500;
    }

    @Override
    public ParticleColor getCenterColor() {
        return new ParticleColor(70, 200, 230);
    }

    @Override
    public boolean canStart(@Nullable Player player) {
        if (player == null || player.level().isClientSide()) {
            return player != null;
        }
        int rank = Necro.rank(player);
        if (rank < Necro.NOVICE) {
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.ritual.not_novice"));
            return false;
        }
        if (rank >= Necro.OVERSEER) {
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.ritual.already"));
            return false;
        }
        if (Souls.total(player) < SOULS) {
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.ritual.need_souls", SOULS));
            return false;
        }
        return true;
    }

    @Override
    protected void tick() {
        if (!(getWorld() instanceof ServerLevel level) || getPos() == null || level.getGameTime() % 20 != 0) {
            return;
        }
        incrementProgress();
        BlockPos pos = getPos();
        if (getProgress() == 1) {
            setNeedsSource(true);
            return;
        }
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 16, 1.2, 0.6, 1.2, 0.02);
        if (getProgress() < 6) {
            return;
        }
        ServerPlayer player = playerUUID == null ? null : level.getServer().getPlayerList().getPlayer(playerUUID);
        if (player != null && awaken(player)) {
            level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4F, 1.6F);
            level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.ritual.awakened"));
        }
        setFinished();
    }

    /** Takes ten souls, weakest first, and makes the player an Overseer. */
    public static boolean awaken(Player player) {
        if (Necro.rank(player) < Necro.NOVICE || Necro.rank(player) >= Necro.OVERSEER || Souls.total(player) < SOULS) {
            return false;
        }
        int left = SOULS;
        for (int strength = 1; strength <= 3 && left > 0; strength++) {
            for (SoulType type : SoulType.values()) {
                while (type.strength == strength && left > 0 && Souls.take(player, type)) {
                    left--;
                }
            }
        }
        Necro.setRank(player, Necro.OVERSEER);
        return true;
    }
}
