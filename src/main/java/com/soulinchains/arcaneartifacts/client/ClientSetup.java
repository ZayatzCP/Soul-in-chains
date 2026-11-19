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
        event.registerEntityRenderer(ModEntities.MUDZYA.get(), CreatureRenderers.MudzyaRenderer::new);
        event.registerEntityRenderer(ModEntities.GRIBUSHKA.get(), context -> new CreatureRenderers.MycelinoidRenderer<>(context,
                "mycelinoid", 1.09F, 0.5F, "mycelinoid_a", "mycelinoid_b"));
        event.registerEntityRenderer(ModEntities.AMANITOID.get(), context -> new CreatureRenderers.MycelinoidRenderer<>(context,
                new String[]{"amanitoid", "amanitoid_b"}, 0.98F, 0.6F, "amanitoid_a", "amanitoid_b"));
        event.registerEntityRenderer(ModEntities.ELDER_MYCELINOID.get(), context -> new CreatureRenderers.MycelinoidRenderer<>(context,
                "elder_mycelinoid", 0.96F, 0.5F, "elder_mycelinoid"));
        event.registerEntityRenderer(ModEntities.GRIBULKA.get(), CreatureRenderers.GribulkaRenderer::new);
        event.registerEntityRenderer(ModEntities.GRIBULKA_HIVE.get(), CreatureRenderers.GribulkaHiveRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_BROOM.get(), context -> new MountRenderer<>(context, "flying_broom", 0.4F));
        event.registerEntityRenderer(ModEntities.MOSS_ELK.get(), context -> new MountRenderer<>(context, "moss_elk", 0.8F));
        event.registerEntityRenderer(ModEntities.IRON_HORSE.get(), context -> new MountRenderer<>(context, "iron_horse", 0.8F));
        event.registerEntityRenderer(ModEntities.SPORE_CLOUD.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_ORB.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 0.8F, true));
        event.registerEntityRenderer(com.soulinchains.arcaneartifacts.catto.CattoContent.CATTO.get(), com.soulinchains.arcaneartifacts.client.catto.CattoRenderer::new);
        event.registerEntityRenderer(ModEntities.SOUL_WISP.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.CHAIN_BOLT.get(), com.soulinchains.arcaneartifacts.client.necro.ChainBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.SHACKLE_ANCHOR.get(), com.soulinchains.arcaneartifacts.client.necro.ShackleAnchorRenderer::new);
        event.registerEntityRenderer(ModEntities.RESTLESS.get(), com.soulinchains.arcaneartifacts.client.necro.NecroMobRenderers.RestlessRenderer::new);
        event.registerEntityRenderer(ModEntities.SOUL_EATER.get(), com.soulinchains.arcaneartifacts.client.necro.NecroMobRenderers.SoulEaterRenderer::new);
        event.registerEntityRenderer(ModEntities.MOURNER.get(), com.soulinchains.arcaneartifacts.client.necro.NecroMobRenderers.MournerRenderer::new);
        event.registerEntityRenderer(ModEntities.BOND_LINK.get(), com.soulinchains.arcaneartifacts.client.necro.BondLinkRenderer::new);
        ModEntities.SERVANTS.forEach((kind, type) -> {
            switch (kind) {
                case HOUND -> event.registerEntityRenderer(type.get(), com.soulinchains.arcaneartifacts.client.necro.ServantRenderers.Hound::new);
                case ROWER -> event.registerEntityRenderer(type.get(), c -> new com.soulinchains.arcaneartifacts.client.necro.ServantRenderers.Humanoid(c, true));
                default -> event.registerEntityRenderer(type.get(), c -> new com.soulinchains.arcaneartifacts.client.necro.ServantRenderers.Humanoid(c, false));
            }
        });
        event.registerEntityRenderer(ModEntities.BULLET.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 0.5F, true));
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MudzyaModel.LAYER, MudzyaModel::createLayer);
        event.registerLayerDefinition(com.soulinchains.arcaneartifacts.client.catto.CattoModel.LAYER, com.soulinchains.arcaneartifacts.client.catto.CattoModel::createLayer);
        event.registerLayerDefinition(com.soulinchains.arcaneartifacts.client.catto.CattoGearLayer.LAYER, com.soulinchains.arcaneartifacts.client.catto.CattoGearLayer::createLayer);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.soulinchains.arcaneartifacts.registry.ModBlocks.RESEARCH_TABLE_ENTITY.get(), ResearchTableRenderer::new);
        event.registerBlockEntityRenderer(com.soulinchains.arcaneartifacts.registry.ModBlocks.MANA_TANK_ENTITY.get(), ManaTankRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.soulinchains.arcaneartifacts.registry.ModBlocks.RESEARCH_TABLE_MENU.get(), ResearchTableScreen::new);
        event.register(com.soulinchains.arcaneartifacts.belt.BeltRegistry.MENU.get(), BeltScreen::new);
        event.register(com.soulinchains.arcaneartifacts.registry.ModBlocks.ALCHEMICAL_DISTILLER_MENU.get(), AlchemicalDistillerScreen::new);
        event.register(com.soulinchains.arcaneartifacts.pseudo.PseudomageRegistry.BANDOLIER_MENU.get(), BandolierScreen::new);
        event.register(ModEntities.IRON_HORSE_MENU.get(), IronHorseScreen::new);
        event.register(com.soulinchains.arcaneartifacts.catto.CattoContent.MENU.get(), com.soulinchains.arcaneartifacts.client.catto.CattoScreen::new);
        event.register(com.soulinchains.arcaneartifacts.registry.ModBlocks.GUNSMITH_MENU.get(), GunsmithScreen::new);
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
            net.minecraft.client.renderer.item.ItemProperties.register(ModItems.PHYLACTERY_AMULET.get(), ArcaneArtifacts.id("style"),
                    (stack, level, entity, seed) -> com.soulinchains.arcaneartifacts.necro.PhylacteryAmuletItem.style(stack) / 10.0F);
            net.minecraft.client.renderer.item.ItemProperties.register(ModItems.MUSHROOM_SHIELD.get(), net.minecraft.resources.ResourceLocation.withDefaultNamespace("blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            net.minecraft.client.renderer.item.ItemProperties.register(ModItems.BANDOLIER.get(), ArcaneArtifacts.id("filled"),
                    (stack, level, entity, seed) -> (float) com.soulinchains.arcaneartifacts.pseudo.BandolierItem.cartridges(stack)
                            / com.soulinchains.arcaneartifacts.pseudo.BandolierItem.CAPACITY);
            for (var shield : java.util.List.of(ModItems.MIRROR_SHIELD.get(), ModItems.FOCUSING_SHIELD.get())) {
                net.minecraft.client.renderer.item.ItemProperties.register(shield, net.minecraft.resources.ResourceLocation.withDefaultNamespace("blocking"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
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

    /** The revolver and the sawn-off are held up and aimed like a loaded crossbow. */
    @SubscribeEvent
    public static void registerItemExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,
                                                                                 net.minecraft.world.InteractionHand hand,
                                                                                 net.minecraft.world.item.ItemStack stack) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        }, ModItems.REVOLVER.get(), ModItems.SAWN_OFF.get());
    }

    @SubscribeEvent
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(BeltKey.KEY);
        event.register(BeltKey.AMULET_KEY);
        event.register(BeltKey.BANDOLIER_KEY);
    }
}
