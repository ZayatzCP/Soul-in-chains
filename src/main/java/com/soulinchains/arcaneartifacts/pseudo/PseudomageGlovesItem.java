package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Pseudomage's Gloves (hands slot): a gunslinger's leather gloves. With them the revolver and the sawn-off
 * fire 40% more often, reload twice as fast and scatter 40% less.
 */
public class PseudomageGlovesItem extends ArcaneAccessoryItem {
    public static final float COOLDOWN = 0.6F;
    public static final float RELOAD = 0.5F;
    public static final float SPREAD = 0.6F;

    public PseudomageGlovesItem() {
        super(new Item.Properties().rarity(Rarity.RARE), 3);
    }

    public static boolean isWearing(LivingEntity entity) {
        return CurioHelper.findEquipped(entity, ModItems.PSEUDOMAGE_GLOVES.get()).isPresent();
    }

    /** A gun's cooldown (ticks) for this shooter. */
    public static int scale(LivingEntity shooter, int ticks, float factor) {
        return isWearing(shooter) ? Math.max(1, Math.round(ticks * factor)) : ticks;
    }
}
