package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Which 3D model is drawn on which body part when an accessory is worn.
 * <p>
 * Every worn model is a plain Blockbench "Java Block/Item" model stored at
 * {@code assets/arcane_artifacts/models/item/worn/<name>.json}. Replacing those files (see MODELS.md and
 * tools/import_models.py) changes what players see, without touching code.
 * <p>
 * Model convention (1 model pixel = 1 player-skin pixel):
 * <ul>
 *     <li>the point (8, 8, 8) of the model is the joint of the body part (neck for head and body,
 *     shoulder for arms, hip for legs);</li>
 *     <li>north (-Z) is the front of the player, east (+X) is the player's right side, +Y is up.</li>
 * </ul>
 */
public final class WornModels {
    public enum BodyPart { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

    public record Attachment(BodyPart part, ResourceLocation model) {
        public ModelResourceLocation location() {
            return ModelResourceLocation.standalone(model);
        }
    }

    private static final Map<Supplier<? extends Item>, List<Attachment>> ATTACHMENTS = new LinkedHashMap<>();

    static {
        ATTACHMENTS.put(ModItems.WREATH_OF_MANIPULATION, List.of(
                worn(BodyPart.HEAD, "wreath_of_manipulation")));
        ATTACHMENTS.put(ModItems.FOCUSING_BOOTS, List.of(
                worn(BodyPart.RIGHT_LEG, "focusing_boots_right"),
                worn(BodyPart.LEFT_LEG, "focusing_boots_left")));
        ATTACHMENTS.put(ModItems.FOCUSING_GLOVES, List.of(
                worn(BodyPart.RIGHT_ARM, "focusing_gloves_right"),
                worn(BodyPart.LEFT_ARM, "focusing_gloves_left")));
        ATTACHMENTS.put(ModItems.RING_OF_REVERSAL, List.of(
                worn(BodyPart.RIGHT_ARM, "ring_of_reversal")));
        ATTACHMENTS.put(ModItems.MANTLE_OF_REFLECTION, List.of(
                worn(BodyPart.BODY, "mantle_of_reflection")));
    }

    private WornModels() {
    }

    private static Attachment worn(BodyPart part, String name) {
        return new Attachment(part, ArcaneArtifacts.id("item/worn/" + name));
    }

    public static Map<Supplier<? extends Item>, List<Attachment>> all() {
        return ATTACHMENTS;
    }
}
