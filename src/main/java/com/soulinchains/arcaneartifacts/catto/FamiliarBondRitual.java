package com.soulinchains.arcaneartifacts.catto;

import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * "Familiar Bond" on the Ritual Brazier: an Arcane Collar is burnt in the brazier, then the ritual turns the owner's tamed
 * cat (within 6 blocks) into Catto. If the owner's Catto has fallen, the same ritual brings it back with everything it
 * knew and carried, and the cat is not needed.
 */
public class FamiliarBondRitual extends AbstractRitual {
    public static final String ID = "ritual_familiar_bond";
    public static final int SOURCE_COST = 1000;

    @Override
    public ResourceLocation getRegistryName() {
        return ArcaneArtifacts.id(ID);
    }

    @Override
    public String getLangName() {
        return "Familiar Bond";
    }

    @Override
    public String getLangDescription() {
        return "Burn an Arcane Collar in the brazier, start the ritual with your tamed cat nearby: the cat becomes Catto, your familiar. "
                + "If your Catto has fallen, the ritual brings it back instead.";
    }

    @Override
    public int getSourceCost() {
        return SOURCE_COST;
    }

    @Override
    public boolean canConsumeItem(ItemStack stack) {
        return stack.is(CattoContent.ARCANE_COLLAR.get()) && !didConsumeItem(CattoContent.ARCANE_COLLAR.get());
    }

    @Override
    public boolean canStart(@Nullable Player player) {
        boolean ready = didConsumeItem(CattoContent.ARCANE_COLLAR.get());
        if (!ready && player != null && !player.level().isClientSide()) {
            player.sendSystemMessage(Component.translatable("arcane_artifacts.catto.ritual.need_collar"));
        }
        return ready;
    }

    @Override
    protected void tick() {
        if (!(getWorld() instanceof ServerLevel level) || getPos() == null || level.getGameTime() % 20 != 0) {
            return;
        }
        incrementProgress();
        BlockPos pos = getPos();
        if (getProgress() == 1) {
            // The brazier takes the Source before the next tick reaches us.
            setNeedsSource(true);
            return;
        }
        level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 1.5, 0.4, 1.5, 0.02);
        if (getProgress() < 5) {
            return;
        }
        if (!perform(level, pos)) {
            // Nothing to bond: the collar is given back.
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    new ItemStack(CattoContent.ARCANE_COLLAR.get())));
        }
        setFinished();
    }

    private boolean perform(ServerLevel level, BlockPos pos) {
        ServerPlayer player = playerUUID == null ? null : level.getServer().getPlayerList().getPlayer(playerUUID);
        CattoSavedData data = CattoSavedData.get(level.getServer());
        if (playerUUID != null) {
            CattoSavedData.Entry entry = data.entry(playerUUID);
            if (entry != null && entry.status == CattoSavedData.Status.FALLEN && entry.snapshot != null) {
                return revive(level, pos, playerUUID, entry.snapshot, player);
            }
            if (entry != null && entry.status == CattoSavedData.Status.IN_BELL) {
                say(player, "arcane_artifacts.catto.ritual.already");
                return false;
            }
            if (entry != null && entry.status == CattoSavedData.Status.ALIVE && entry.catId != null && Catto.findById(level.getServer(), entry.catId) != null) {
                say(player, "arcane_artifacts.catto.ritual.already");
                return false;
            }
        }
        List<Cat> cats = level.getEntitiesOfClass(Cat.class, new AABB(pos).inflate(6), c -> !(c instanceof Catto) && c.isAlive() && c.isTame()
                && (playerUUID == null ? c.getOwnerUUID() != null : playerUUID.equals(c.getOwnerUUID())));
        if (cats.isEmpty()) {
            say(player, "arcane_artifacts.catto.ritual.no_cat");
            return false;
        }
        Cat cat = cats.get(0);
        UUID owner = cat.getOwnerUUID();
        if (playerUUID == null && owner != null) {
            CattoSavedData.Entry entry = data.entry(owner);
            if (entry != null && entry.status != CattoSavedData.Status.FALLEN) {
                return false;
            }
        }
        CompoundTag tag = cat.saveWithoutId(new CompoundTag());
        tag.remove("UUID");
        tag.putByte("CollarColor", (byte) DyeColor.PURPLE.getId());
        Catto catto = CattoContent.CATTO.get().create(level);
        if (catto == null || owner == null) {
            return false;
        }
        catto.load(tag);
        catto.moveTo(cat.getX(), cat.getY(), cat.getZ(), cat.getYRot(), cat.getXRot());
        catto.setOwnerUUID(owner);
        catto.setPersistenceRequired();
        cat.discard();
        level.addFreshEntity(catto);
        data.alive(owner, catto);
        celebrate(level, catto);
        ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
        say(ownerPlayer, "arcane_artifacts.catto.ritual.bonded");
        return true;
    }

    private boolean revive(ServerLevel level, BlockPos pos, UUID owner, CompoundTag snapshot, @Nullable ServerPlayer player) {
        Catto catto = CattoContent.CATTO.get().create(level);
        if (catto == null) {
            return false;
        }
        CompoundTag tag = snapshot.copy();
        tag.remove("UUID");
        catto.load(tag);
        catto.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
        catto.setHealth(catto.getMaxHealth());
        catto.clearFire();
        catto.removeAllEffects();
        catto.setOwnerUUID(owner);
        level.addFreshEntity(catto);
        CattoSavedData.get(level.getServer()).alive(owner, catto);
        celebrate(level, catto);
        say(player, "arcane_artifacts.catto.ritual.revived");
        return true;
    }

    private static void celebrate(ServerLevel level, Catto catto) {
        level.sendParticles(ParticleTypes.HEART, catto.getX(), catto.getY() + 0.6, catto.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, catto.getX(), catto.getY() + 0.4, catto.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, catto.blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private static void say(@Nullable ServerPlayer player, String key) {
        if (player != null) {
            player.sendSystemMessage(Component.translatable(key));
        }
    }
}
