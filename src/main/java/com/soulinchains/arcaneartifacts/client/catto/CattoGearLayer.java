package com.soulinchains.arcaneartifacts.client.catto;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.catto.Catto;
import com.soulinchains.arcaneartifacts.catto.CattoContent;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Draws what Catto wears: the arcane hood, the sombrerito, the herbalist wreath, the mantle, the ponchito, the cloak,
 * and plain helmets and chestplates (tinted with their material). Nothing worn, nothing drawn.
 */
public class CattoGearLayer extends RenderLayer<Catto, CattoModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcaneArtifacts.id("catto_gear"), "main");

    private final GearModel gear;

    public CattoGearLayer(RenderLayerParent<Catto, CattoModel> parent, EntityModelSet models) {
        super(parent);
        this.gear = new GearModel(models.bakeLayer(LAYER));
    }

    /** One texture sheet per piece, all on the layout of {@link #createLayer()}. */
    private record Look(ResourceLocation texture, Set<String> parts, int color) {
    }

    private static ResourceLocation tex(String name) {
        return ArcaneArtifacts.id("textures/entity/catto_gear/" + name + ".png");
    }

    private static Look look(ItemStack stack, boolean head) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(CattoContent.ARCANE_HOOD.get())) {
            return new Look(tex("arcane_catto_hood"), Set.of("hood", "hood_right_ear", "hood_left_ear"), -1);
        }
        if (stack.is(CattoContent.SOMBRERITO.get())) {
            return new Look(tex("catto_sombrero"), Set.of("brim", "crown"), -1);
        }
        if (stack.is(CattoContent.DRUID_WREATH.get())) {
            return new Look(tex("catto_druid_wreath"), Set.of("wreath"), -1);
        }
        if (stack.is(CattoContent.ARCANE_MANTLE.get())) {
            return new Look(tex("arcane_catto_mantle"), Set.of("cloak", "cape"), -1);
        }
        if (stack.is(CattoContent.PONCHITO.get())) {
            return new Look(tex("catto_poncho"), Set.of("cloak"), -1);
        }
        if (stack.is(CattoContent.DRUID_CLOAK.get())) {
            return new Look(tex("catto_druid_cloak"), Set.of("cloak", "cape"), -1);
        }
        if (stack.getItem() instanceof ArmorItem armor) {
            return new Look(tex("plain_metal"), Set.of(head ? "helmet" : "vest"), materialColor(stack, armor));
        }
        return null;
    }

    private static int materialColor(ItemStack stack, ArmorItem armor) {
        var dyed = stack.get(DataComponents.DYED_COLOR);
        if (dyed != null) {
            return 0xFF000000 | dyed.rgb();
        }
        String material = armor.getMaterial().unwrapKey().map(key -> key.location().getPath()).orElse("iron");
        return switch (material) {
            case "leather" -> 0xFFA0653A;
            case "chainmail" -> 0xFF9A9A9A;
            case "gold" -> 0xFFF0D040;
            case "diamond" -> 0xFF5FE0D8;
            case "netherite" -> 0xFF4A4044;
            case "turtle" -> 0xFF4AA040;
            default -> 0xFFD8D8D8;
        };
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, Catto cat, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) {
            return;
        }
        gear.head.copyFrom(getParentModel().head());
        gear.body.copyFrom(getParentModel().body());
        for (Look look : new Look[]{look(cat.getItemBySlot(EquipmentSlot.HEAD), true), look(cat.getItemBySlot(EquipmentSlot.CHEST), false)}) {
            if (look == null) {
                continue;
            }
            gear.show(look.parts());
            gear.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(look.texture())), light, OverlayTexture.NO_OVERLAY,
                    look.color() == -1 ? 0xFFFFFFFF : look.color());
        }
    }

    /** The pieces of every outfit, hidden except the ones being drawn. Shares the base model's head and body poses. */
    static final class GearModel extends EntityModel<Catto> {
        final ModelPart root;
        final ModelPart head;
        final ModelPart body;

        GearModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.body = root.getChild("body");
        }

        void show(Set<String> parts) {
            for (ModelPart group : new ModelPart[]{head, body}) {
                group.getAllParts().forEach(part -> part.visible = true);
            }
            for (String name : new String[]{"hood", "hood_right_ear", "hood_left_ear", "brim", "crown", "wreath", "helmet"}) {
                head.getChild(name).visible = parts.contains(name);
            }
            for (String name : new String[]{"cloak", "cape", "vest"}) {
                body.getChild(name).visible = parts.contains(name);
            }
        }

        @Override
        public void setupAnim(Catto cat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        }

        @Override
        public void renderToBuffer(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, int color) {
            head.render(pose, buffer, light, overlay, color);
            body.render(pose, buffer, light, overlay, color);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        head.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -6.5F, -3.5F, 8.0F, 7.0F, 7.0F), PartPose.ZERO);
        head.addOrReplaceChild("hood_right_ear", CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-2.3F, -6.3F, 0.0F, 0.0F, 0.0F, -0.25F));
        head.addOrReplaceChild("hood_left_ear", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-1.5F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(2.3F, -6.3F, 0.0F, 0.0F, 0.0F, 0.25F));
        head.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0F, -6.6F, -6.0F, 12.0F, 1.0F, 12.0F), PartPose.ZERO);
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(32, 6).addBox(-2.5F, -9.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.ZERO);
        head.addOrReplaceChild("wreath", CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -6.4F, -3.5F, 8.0F, 1.0F, 7.0F), PartPose.ZERO);
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(34, 30).addBox(-4.0F, -6.6F, -3.5F, 8.0F, 4.0F, 7.0F), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));
        body.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 40).addBox(-3.5F, -7.3F, -2.5F, 7.0F, 8.0F, 5.0F), PartPose.ZERO);
        body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(42, 42).addBox(-3.0F, -7.0F, 4.1F, 6.0F, 8.0F, 1.0F), PartPose.ZERO);
        body.addOrReplaceChild("vest", CubeListBuilder.create().texOffs(26, 52).addBox(-3.5F, -7.2F, -2.5F, 7.0F, 6.0F, 5.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
