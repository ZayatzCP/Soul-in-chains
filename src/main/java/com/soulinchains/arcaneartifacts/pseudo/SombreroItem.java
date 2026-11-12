package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Pseudomage's Sombrero (head slot): works like the Pseudomage's Goggles (exact Source in tanks and jars)
 * and gives a passive 15% chance to deal a critical hit (+50% damage) with anything.
 */
public class SombreroItem extends ArcaneAccessoryItem {
    public static final float CRIT_CHANCE = 0.15f;
    public static final float CRIT_MULTIPLIER = 1.5f;

    public SombreroItem() {
        super(new Item.Properties().rarity(Rarity.RARE), 2);
    }

    @EventBusSubscriber(modid = ArcaneArtifacts.MODID)
    public static final class Crits {
        private Crits() {
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onHurt(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == event.getEntity()
                    || CurioHelper.findEquipped(attacker, ModItems.SOMBRERO.get()).isEmpty()
                    || attacker.getRandom().nextFloat() >= CRIT_CHANCE) {
                return;
            }
            event.setAmount(event.getAmount() * CRIT_MULTIPLIER);
            LivingEntity target = event.getEntity();
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }
}
