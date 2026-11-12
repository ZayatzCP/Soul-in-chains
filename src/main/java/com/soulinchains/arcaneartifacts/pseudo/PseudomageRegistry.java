package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Menus of the Pseudomage (registered with the mod's other menus). */
public final class PseudomageRegistry {
    public static final DeferredHolder<MenuType<?>, MenuType<BandolierMenu>> BANDOLIER_MENU =
            ModBlocks.MENUS.register("bandolier", () -> new MenuType<>(BandolierMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private PseudomageRegistry() {
    }

    /** Forces class loading so the menu is registered. */
    public static void init() {
        ArcaneArtifacts.LOGGER.debug("Pseudomage menus registered");
    }
}
