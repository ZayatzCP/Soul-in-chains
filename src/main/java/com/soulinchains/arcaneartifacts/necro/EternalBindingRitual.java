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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Eternal Binding (Chainlord, Ritual Brazier): the nearest of your servants within 6 blocks is chained for good. It no
 * longer crumbles when its chain is stretched - it waits for you - and it grows with its kills (up to level 10, +5%
 * health and damage each). Costs a cursed soul. At most 3 bound servants.
 */
public class EternalBindingRitual extends AbstractRitual {
    public static final String ID = "ritual_eternal_binding";

    @Override
    public ResourceLocation getRegistryName() {
        return ArcaneArtifacts.id(ID);
    }

    @Override
    public String getLangName() {
        return "Eternal Binding";
    }

    @Override
    public String getLangDescription() {
        return "Chainlord rank. Your nearest servant within 6 blocks is chained for good: it never crumbles, waits for you, and grows with its kills. Costs a cursed soul; at most 3 bound servants.";
    }

    @Override
    public int getSourceCost() {
        return 1000;
    }

    @Override
    public ParticleColor getCenterColor() {
        return new ParticleColor(150, 230, 255);
    }

    @Override
    public boolean canStart(@Nullable Player player) {
        if (player == null || player.level().isClientSide()) {
            return player != null;
        }
        if (Necro.rank(player) < Necro.CHAINLORD) {
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.rank_needed", Component.translatable("arcane_artifacts.necro.rank.3")));
            return false;
        }
        if (Souls.count(player, SoulType.CURSED) < 1) {
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.ritual.need_cursed"));
            return false;
        }
        if (Servant.boundCount(player.getUUID()) >= Servant.MAX_BOUND) {
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.ritual.bound_limit", Servant.MAX_BOUND));
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
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 10, 1.5, 0.4, 1.5, 0.02);
        if (getProgress() < 6) {
            return;
        }
        ServerPlayer player = playerUUID == null ? null : level.getServer().getPlayerList().getPlayer(playerUUID);
        if (player != null) {
            Servant servant = bind(level, pos, player);
            player.sendSystemMessage(servant != null
                    ? Component.translatable("arcane_artifacts.necro.ritual.bound", servant.getDisplayName())
                    : Component.translatable("arcane_artifacts.necro.ritual.no_servant"));
        }
        setFinished();
    }

    /** Binds the master's nearest unbound servant around {@code pos} for good. */
    @Nullable
    public static Servant bind(ServerLevel level, BlockPos pos, Player master) {
        if (Servant.boundCount(master.getUUID()) >= Servant.MAX_BOUND) {
            return null;
        }
        Servant servant = level.getEntitiesOfClass(Servant.class, new AABB(pos).inflate(6),
                        s -> master.getUUID().equals(s.getOwnerUUID()) && !s.isBound()).stream()
                .min(Comparator.comparingDouble(s -> s.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))).orElse(null);
        if (servant == null || !Souls.spend(master, SoulType.CURSED)) {
            return null;
        }
        servant.bindForever();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, servant.getX(), servant.getY() + 1, servant.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        level.playSound(null, servant.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 0.6F);
        return servant;
    }
}
