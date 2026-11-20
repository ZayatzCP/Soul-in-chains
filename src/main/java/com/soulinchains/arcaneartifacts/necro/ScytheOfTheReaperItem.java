package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Scythe of the Reaper (relic weapon, from the Imprisoned King): every blow sweeps a half circle in front of you
 * (60% damage to everyone else there). Right click - Harvest: 6 damage to every foe within 8 blocks, and whoever dies in
 * the next three seconds sends its soul straight into your lantern. Once every 90 seconds.
 */
public class ScytheOfTheReaperItem extends SwordItem {
    public static final double ARC_RANGE = 4.0;
    public static final float ARC_SHARE = 0.6F;
    public static final double HARVEST_RANGE = 8.0;
    public static final float HARVEST_DAMAGE = 6F;
    public static final int HARVEST_COOLDOWN = 1800;
    private static boolean sweeping;

    public ScytheOfTheReaperItem() {
        super(Tiers.NETHERITE, new Properties().rarity(Rarity.EPIC).fireResistant()
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.8F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!sweeping && attacker instanceof Player player && player.level() instanceof ServerLevel level) {
            sweeping = true;
            try {
                arc(level, player, target);
            } finally {
                sweeping = false;
            }
        }
        return result;
    }

    /** The half circle in front of the reaper. */
    public static int arc(ServerLevel level, Player player, LivingEntity struck) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        float damage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * ARC_SHARE;
        int hit = 0;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ARC_RANGE),
                e -> e != player && e != struck && e.isAlive() && foe(player, e))) {
            Vec3 to = other.position().subtract(player.position()).multiply(1, 0, 1);
            if (to.lengthSqr() > ARC_RANGE * ARC_RANGE || to.normalize().dot(look) < 0) {
                continue;
            }
            other.hurt(level.damageSources().playerAttack(player), damage);
            hit++;
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + look.x * 2, player.getY() + 1, player.getZ() + look.z * 2, 3, 1, 0.1, 1, 0);
        return hit;
    }

    static boolean foe(Player player, LivingEntity e) {
        if (e.isAlliedTo(player) || player.isAlliedTo(e)) {
            return false;
        }
        if (e instanceof Servant servant && player.getUUID().equals(servant.getOwnerUUID())) {
            return false;
        }
        return !(e instanceof TamableAnimal pet && pet.isOwnedBy(player));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            harvest(server, player);
            stack.hurtAndBreak(5, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, HARVEST_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Harvest: marks every foe around for reaping, then cuts them. Returns how many were cut. */
    public static int harvest(ServerLevel level, Player player) {
        List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(HARVEST_RANGE),
                e -> e != player && e.isAlive() && foe(player, e) && e.distanceToSqr(player) <= HARVEST_RANGE * HARVEST_RANGE);
        for (LivingEntity foe : foes) {
            foe.addEffect(new MobEffectInstance(ModEffects.REAP_MARK, 60, 0, false, true, true), player);
            Souls.markForReaping(foe, player.getUUID(), false);
            foe.invulnerableTime = 0;
            foe.hurt(level.damageSources().indirectMagic(player, player), HARVEST_DAMAGE);
        }
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2 / 24;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX() + Math.cos(angle) * HARVEST_RANGE * 0.6, player.getY() + 0.8,
                    player.getZ() + Math.sin(angle) * HARVEST_RANGE * 0.6, 2, 0.2, 0.1, 0.2, 0.01);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 0.5F);
        return foes.size();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.2").withStyle(ChatFormatting.DARK_AQUA));
    }
}
