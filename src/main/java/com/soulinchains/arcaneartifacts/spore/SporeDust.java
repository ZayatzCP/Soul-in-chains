package com.soulinchains.arcaneartifacts.spore;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/** The spore damage a Mushroom Club leaves behind, dealt once the blow's invulnerability is over. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class SporeDust {
    static final int DELAY = 11;
    private static final Map<LivingEntity, Integer> PENDING = new WeakHashMap<>();

    private SporeDust() {
    }

    static void settle(LivingEntity target) {
        PENDING.put(target, DELAY);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        java.util.List<LivingEntity> due = new java.util.ArrayList<>();
        Iterator<Map.Entry<LivingEntity, Integer>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Integer> entry = it.next();
            LivingEntity entity = entry.getKey();
            if (entity == null || !entity.isAlive()) {
                it.remove();
            } else if (entry.getValue() <= 1) {
                it.remove();
                due.add(entity);
            } else {
                entry.setValue(entry.getValue() - 1);
            }
        }
        due.forEach(entity -> entity.hurt(entity.damageSources().magic(), MushroomClubItem.SPORE_DAMAGE));
    }
}
