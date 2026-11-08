package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Rendered exactly like an Ars Nouveau spell projectile (spell style + particle timeline).
        event.registerEntityRenderer(ModEntities.ARCANE_MISSILE.get(), com.hollingsworth.arsnouveau.client.renderer.entity.StyledSpellRender::new);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.soulinchains.arcaneartifacts.registry.ModBlocks.RESEARCH_TABLE_ENTITY.get(), ResearchTableRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.soulinchains.arcaneartifacts.registry.ModBlocks.RESEARCH_TABLE_MENU.get(), ResearchTableScreen::new);
        event.register(com.soulinchains.arcaneartifacts.belt.BeltRegistry.MENU.get(), BeltScreen::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(WreathColors::color, ModItems.WREATH_OF_MANIPULATION.get());
    }

    @SubscribeEvent
    public static void registerWornModels(ModelEvent.RegisterAdditional event) {
        WornModels.all().values().forEach(list -> list.forEach(attachment -> event.register(attachment.location())));
        WornModels.GLOVES.forEach(attachment -> event.register(attachment.location()));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            WornModels.all().forEach((item, attachments) ->
                    CuriosRendererRegistry.register(item.get(), () -> new AccessoryCurioRenderer(attachments)));
            net.minecraft.client.renderer.item.ItemProperties.register(ModItems.AMULET_OF_OPPOSITES.get(), ArcaneArtifacts.id("repelling"),
                    (stack, level, entity, seed) -> com.soulinchains.arcaneartifacts.accessory.AmuletOfOppositesItem.isRepelling(stack) ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
                renderer.addLayer(new GlovesRenderer.Layer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(BeltKey.KEY);
    }
}
