package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.accessory.DamageCategory;
import com.soulinchains.arcaneartifacts.accessory.WreathData;
import com.soulinchains.arcaneartifacts.accessory.WreathOfManipulationItem;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Flower colours of the wreath. Tint index 0 is the vine (untinted); indexes 1-4 are the flower groups
 * physical / fire / water / magic. A dormant group is pale grey, a group gathering hits gradually
 * takes its colour, and an adapted group shines in full colour (pulsing slightly).
 */
public final class WreathColors {
    private static final int DORMANT = 0x8E8A99;
    private static final int[] GROUP_COLORS = {
            0xD9DEE8, // physical: silver-white
            0xFF8A1F, // fire & lava: ember orange
            0x2FD6E0, // suffocation & drowning: turquoise
            0xB548F5  // magic: arcane violet
    };

    private WreathColors() {
    }

    public static int color(ItemStack stack, int tintIndex) {
        if (tintIndex <= 0 || tintIndex > GROUP_COLORS.length) {
            return 0xFFFFFFFF;
        }
        int group = tintIndex - 1;
        WreathData data = WreathOfManipulationItem.data(stack);
        Level level = ClientAccess.level();
        long now = level == null ? 0 : level.getGameTime();
        int needed = Config.SPEC.isLoaded() ? Config.WREATH_HITS_TO_ADAPT.get() : 5;
        boolean adapted = false;
        int hits = 0;
        for (DamageCategory category : DamageCategory.values()) {
            if (category.flowerGroup != group) continue;
            adapted |= data.isAdapted(category, now);
            hits = Math.max(hits, data.hits(category));
        }
        int target = GROUP_COLORS[group];
        if (adapted) {
            float pulse = 0.85f + 0.15f * (float) Math.sin(now / 6.0);
            return 0xFF000000 | lerp(0xFFFFFF, target, pulse);
        }
        float progress = Math.min(1f, hits / (float) Math.max(1, needed)) * 0.6f;
        return 0xFF000000 | lerp(DORMANT, target, progress);
    }

    private static int lerp(int from, int to, float t) {
        int r = (int) (FastColor.ARGB32.red(from) + (FastColor.ARGB32.red(to) - FastColor.ARGB32.red(from)) * t);
        int g = (int) (FastColor.ARGB32.green(from) + (FastColor.ARGB32.green(to) - FastColor.ARGB32.green(from)) * t);
        int b = (int) (FastColor.ARGB32.blue(from) + (FastColor.ARGB32.blue(to) - FastColor.ARGB32.blue(from)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
