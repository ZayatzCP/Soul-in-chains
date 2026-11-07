package com.soulinchains.arcaneartifacts;

import com.soulinchains.arcaneartifacts.glyph.ArcaneGlyphs;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModCreativeTab;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of "Ars Nouveau: Arcane Artifacts".
 * <p>
 * Glyphs are registered into Ars Nouveau's {@code GlyphRegistry} from the constructor: Ars registers
 * the glyph items for every registered spell part during its own item {@code RegisterEvent}, which
 * fires after all mod constructors have run.
 */
@Mod(ArcaneArtifacts.MODID)
public final class ArcaneArtifacts {
    public static final String MODID = "arcane_artifacts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public ArcaneArtifacts(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        ModComponents.REGISTER.register(modBus);
        ModAttachments.REGISTER.register(modBus);
        ModEffects.REGISTER.register(modBus);
        ModEntities.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModCreativeTab.REGISTER.register(modBus);

        ArcaneGlyphs.registerGlyphs();

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ArcaneGlyphs::postInit);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
