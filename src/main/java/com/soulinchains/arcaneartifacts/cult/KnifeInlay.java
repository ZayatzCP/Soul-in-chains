package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * An inlay in the Ritual Knife's handle, one at a time: bone (anchors take a quarter less from the Torn Acolytes), red
 * glass (rarer summons more often), iron (the rite is a fifth shorter, its windows come faster).
 */
public enum KnifeInlay {
    NONE(null), BONE(Items.BONE_BLOCK), RED_GLASS(Items.RED_STAINED_GLASS), IRON(Items.IRON_BLOCK);

    public final Item material;

    KnifeInlay(Item material) {
        this.material = material;
    }

    public static KnifeInlay of(ItemStack knife) {
        if (!(knife.getItem() instanceof RitualKnifeItem)) {
            return NONE;
        }
        int id = knife.getOrDefault(ModComponents.KNIFE_INLAY, 0);
        KnifeInlay[] all = values();
        return id >= 0 && id < all.length ? all[id] : NONE;
    }

    public static KnifeInlay forMaterial(ItemStack stack) {
        for (KnifeInlay inlay : values()) {
            if (inlay.material != null && stack.is(inlay.material)) {
                return inlay;
            }
        }
        return NONE;
    }

    public Component displayName() {
        return Component.translatable("arcane_artifacts.knife_inlay." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
