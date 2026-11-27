package com.soulinchains.arcaneartifacts.client.astral;

import com.mojang.blaze3d.shaders.FogShape;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.astral.Astral;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * How the Astral looks: no sun, moon, stars or clouds, only the pale haze of the biome's fog colour, and a round fog that
 * starts close, so the monoliths dissolve upwards long before their tops.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AstralClient {
    /** Where the fog starts and ends, as parts of the render distance. */
    static final float FOG_START = 0.12F;
    static final float FOG_END = 0.85F;

    private AstralClient() {
    }

    public static final class Effects extends DimensionSpecialEffects {
        public Effects() {
            super(Float.NaN, false, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
            return color;
        }

        @Override
        public boolean isFoggyAt(int x, int z) {
            return false;
        }
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Astral.EFFECTS, new Effects());
        NeoForge.EVENT_BUS.addListener(AstralClient::fog);
    }

    static void fog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !Astral.isAstral(mc.level) || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        float far = event.getFarPlaneDistance();
        event.setNearPlaneDistance(far * FOG_START);
        event.setFarPlaneDistance(far * FOG_END);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
