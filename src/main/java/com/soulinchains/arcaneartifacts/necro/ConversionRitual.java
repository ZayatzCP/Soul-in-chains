package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
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
 * Conversion (Ritual Brazier): a Chainlord at 99 Corruption, wearing a bound Phylactery Amulet, burns a great soul and
 * becomes a Lich - Corruption 100, for good.
 */
public class ConversionRitual extends AbstractRitual {
    public static final String ID = "ritual_conversion";

    @Override
    public ResourceLocation getRegistryName() {
        return ArcaneArtifacts.id(ID);
    }

    @Override
    public String getLangName() {
        return "Conversion";
    }

    @Override
    public String getLangDescription() {
        return "A Chainlord at 99 Corruption, wearing a bound Phylactery Amulet, burns a great soul and becomes a Lich. There is no way back.";
    }

    @Override
    public int getSourceCost() {
        return 3000;
    }

    @Override
    public ParticleColor getCenterColor() {
        return new ParticleColor(120, 230, 120);
    }

    /** Null when the player may convert, otherwise the reason (a lang key suffix). */
    @Nullable
    public static String refusal(Player player) {
        if (Corruption.isLich(player)) {
            return "already_lich";
        }
        if (Necro.rank(player) < Necro.CHAINLORD) {
            return "need_chainlord";
        }
        if (Corruption.get(player) < Corruption.MAX) {
            return "need_corruption";
        }
        var amulet = CurioHelper.findEquipped(player, ModItems.PHYLACTERY_AMULET.get());
        if (amulet.isEmpty() || PhylacteryAmuletItem.state(amulet.get()).bound().isEmpty()) {
            return "need_phylactery";
        }
        if (Souls.count(player, SoulType.GREAT) <= 0) {
            return "need_great";
        }
        return null;
    }

    @Override
    public boolean canStart(@Nullable Player player) {
        if (player == null || player.level().isClientSide()) {
            return player != null;
        }
        String reason = refusal(player);
        if (reason != null) {
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.ritual." + reason));
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
        level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 1.5, 0.8, 1.5, 0.02);
        if (getProgress() < 10) {
            return;
        }
        ServerPlayer player = playerUUID == null ? null : level.getServer().getPlayerList().getPlayer(playerUUID);
        if (player != null && convert(player)) {
            level.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.6F, 0.5F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 80, 0.6, 1.0, 0.6, 0.05);
            player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.ritual.lich"));
        }
        setFinished();
    }

    public static boolean convert(Player player) {
        if (refusal(player) != null || !Souls.take(player, SoulType.GREAT)) {
            return false;
        }
        Corruption.makeLich(player);
        return true;
    }
}
