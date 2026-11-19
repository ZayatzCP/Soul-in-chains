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

    /**
     * Extra placement for models that the Java model format cannot express: a rotation in degrees (applied
     * X, then Y, then Z) around {@code pivot}, then a move by {@code offset} (both in model pixels).
     * {@code shakesInRush}: the model jitters sideways while the Battle Rush is active.
     */
    public record Placement(float pivotX, float pivotY, float pivotZ, float rotX, float rotY, float rotZ,
                            float offsetX, float offsetY, float offsetZ, boolean shakesInRush, float turnY) {
        public static final Placement NONE = new Placement(0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0);
    }

    public record Attachment(BodyPart part, ResourceLocation model, Placement placement,
                             java.util.function.Predicate<net.minecraft.world.item.ItemStack> when) {
        public Attachment(BodyPart part, ResourceLocation model, Placement placement) {
            this(part, model, placement, stack -> true);
        }

        public ModelResourceLocation location() {
            return ModelResourceLocation.standalone(model);
        }
    }

    private static final Map<Supplier<? extends Item>, List<Attachment>> ATTACHMENTS = new LinkedHashMap<>();

    static {
        // The designer's wreath lies flat; Blockbench rotated it on two axes at once, which 1.21.1 model
        // files cannot store, so the rotation lives here. The offset seats the ring a little above the
        // crown of the head; turnY faces it the other way round.
        ATTACHMENTS.put(ModItems.WREATH_OF_MANIPULATION, List.of(
                new Attachment(BodyPart.HEAD, ArcaneArtifacts.id("item/worn/wreath_of_manipulation"),
                        new Placement(7f, -2.81233f, 8.56392f, -105f, 0f, -180f, 1f, 20.2f, -0.56f, true, 180f))));
        ATTACHMENTS.put(ModItems.FOCUSING_BOOTS, List.of(
                worn(BodyPart.RIGHT_LEG, "focusing_boots_right"),
                worn(BodyPart.LEFT_LEG, "focusing_boots_left")));
        ATTACHMENTS.put(ModItems.RING_OF_REVERSAL, List.of(
                worn(BodyPart.RIGHT_ARM, "ring_of_reversal")));
        ATTACHMENTS.put(ModItems.MANTLE_OF_REFLECTION, List.of(
                worn(BodyPart.BODY, "mantle_of_reflection"),
                worn(BodyPart.HEAD, "mantle_of_reflection_hood"),
                worn(BodyPart.RIGHT_ARM, "mantle_of_reflection_right_sleeve"),
                worn(BodyPart.LEFT_ARM, "mantle_of_reflection_left_sleeve")));
        ATTACHMENTS.put(ModItems.HOP_SLIPPERS, List.of(
                worn(BodyPart.RIGHT_LEG, "hop_slippers_right"),
                worn(BodyPart.LEFT_LEG, "hop_slippers_left")));
        ATTACHMENTS.put(ModItems.PSEUDOMAGE_BOOTS, List.of(
                worn(BodyPart.RIGHT_LEG, "pseudomage_boots_right"),
                worn(BodyPart.LEFT_LEG, "pseudomage_boots_left")));
        ATTACHMENTS.put(ModItems.RING_OF_CALM, List.of(worn(BodyPart.RIGHT_ARM, "ring_of_calm")));
        ATTACHMENTS.put(ModItems.BOTTOMLESS_BELT, List.of(worn(BodyPart.BODY, "bottomless_belt")));
        // Pseudomage (the designer's outfit, tools/generate_batch18.py). Parts the Java model format cannot turn
        // are separate models, turned here: the bandolier's strap runs across the chest at 55 degrees.
        ATTACHMENTS.put(ModItems.BANDOLIER, List.of(
                turned(BodyPart.BODY, "bandolier", 10f, -4f, 8f, 0f, 0f, -55.00933f),
                turned(BodyPart.BODY, "bandolier_2", 9.8504f, -16.5255f, 8f, 0f, 0f, -90f),
                turned(BodyPart.BODY, "bandolier_3", 1.8222f, -5.056f, 8f, 0f, 0f, -90f)));
        // The Druid (the designer's outfit, tools/generate_batch19.py).
        ATTACHMENTS.put(ModItems.DRUID_MANTLE, List.of(
                worn(BodyPart.BODY, "druid_mantle"),
                turned(BodyPart.BODY, "druid_mantle_2", 8.5671f, -2.3896f, 6.8374f, 0.0f, 0.0f, -90.0f),
                worn(BodyPart.HEAD, "druid_mantle_hood"),
                worn(BodyPart.RIGHT_ARM, "druid_mantle_right"),
                worn(BodyPart.LEFT_ARM, "druid_mantle_left")));
        ATTACHMENTS.put(ModItems.DRUID_HAT, List.of(
                worn(BodyPart.HEAD, "druid_hat"),
                turned(BodyPart.HEAD, "druid_hat_2", -15.1147f, 11.7166f, 6.9243f, 7.4366f, 0.9762f, 7.4366f),
                turned(BodyPart.HEAD, "druid_hat_3", -9.4175f, 13.6948f, 6.892f, 7.5f, 0.0f, 0.0f),
                turned(BodyPart.HEAD, "druid_hat_4", -13.2724f, 9.4479f, 0.6845f, 4.6066f, -14.2906f, 18.0778f),
                turned(BodyPart.HEAD, "druid_hat_5", -12.2724f, 9.3658f, 11.013f, -3.0351f, 9.5327f, 17.7531f),
                turned(BodyPart.HEAD, "druid_hat_6", 5.2203f, 17.6701f, 15.013f, -53.7071f, 30.88f, 72.9079f),
                turned(BodyPart.HEAD, "druid_hat_7", 15.8772f, 15.53f, 9.013f, -3.0351f, 9.5327f, -7.2469f),
                turned(BodyPart.HEAD, "druid_hat_8", 2.4508f, 17.9702f, 3.7944f, 7.5f, 90.0f, 0.0f)));
        ATTACHMENTS.put(ModItems.DRUID_GLOVES, List.of(
                worn(BodyPart.RIGHT_ARM, "druid_glove_right"),
                worn(BodyPart.LEFT_ARM, "druid_glove_left")));
        ATTACHMENTS.put(ModItems.PSEUDOMAGE_GLOVES, List.of(
                worn(BodyPart.RIGHT_ARM, "pseudomage_glove_right"),
                worn(BodyPart.LEFT_ARM, "pseudomage_glove_left")));
        ATTACHMENTS.put(ModItems.COUNTERFEIT_RING, List.of(worn(BodyPart.RIGHT_ARM, "counterfeit_ring")));
        ATTACHMENTS.put(ModItems.COUNTERFEIT_PENDANT, List.of(worn(BodyPart.BODY, "counterfeit_pendant")));
        ATTACHMENTS.put(ModItems.MANA_GOGGLES, List.of(worn(BodyPart.HEAD, "mana_goggles")));
        // The hat sits at a jaunty angle: its brim and crown are turned separately.
        ATTACHMENTS.put(ModItems.SOMBRERO, List.of(
                worn(BodyPart.HEAD, "sombrero"),
                turned(BodyPart.HEAD, "sombrero_2", 39.979f, 13.474f, 6.8464f, 10.0f, 0.0f, -4.0f),
                turned(BodyPart.HEAD, "sombrero_3", 44.6436f, 9.6054f, 8f, 0f, 0f, -10f)));
        ATTACHMENTS.put(ModItems.PONCHO, List.of(
                worn(BodyPart.BODY, "poncho"),
                worn(BodyPart.HEAD, "poncho_hood"),
                worn(BodyPart.RIGHT_ARM, "poncho_right"),
                worn(BodyPart.LEFT_ARM, "poncho_left")));
        // The worn amulet follows its polarity, like the icon.
        ATTACHMENTS.put(ModItems.AMULET_OF_OPPOSITES, List.of(
                new Attachment(BodyPart.BODY, ArcaneArtifacts.id("item/worn/amulet_of_opposites"), Placement.NONE,
                        stack -> !com.soulinchains.arcaneartifacts.accessory.AmuletOfOppositesItem.isRepelling(stack)),
                new Attachment(BodyPart.BODY, ArcaneArtifacts.id("item/worn/amulet_of_opposites_repel"), Placement.NONE,
                        com.soulinchains.arcaneartifacts.accessory.AmuletOfOppositesItem::isRepelling)));
        // The Phylactery Amulet hangs on the chest in the look its wearer chose.
        java.util.List<Attachment> phylactery = new java.util.ArrayList<>();
        for (int style = 0; style < com.soulinchains.arcaneartifacts.necro.Phylactery.STYLES; style++) {
            final int s = style;
            phylactery.add(new Attachment(BodyPart.BODY, ArcaneArtifacts.id("item/worn/phylactery_amulet_" + style), Placement.NONE,
                    stack -> com.soulinchains.arcaneartifacts.necro.PhylacteryAmuletItem.style(stack) == s));
        }
        ATTACHMENTS.put(ModItems.PHYLACTERY_AMULET, List.copyOf(phylactery));
        ATTACHMENTS.put(ModItems.SHACKLED_BOOTS, List.of(
                worn(BodyPart.RIGHT_LEG, "shackled_boots_right"),
                worn(BodyPart.LEFT_LEG, "shackled_boots_left")));
        ATTACHMENTS.put(ModItems.HEART_CHARM, List.of(worn(BodyPart.BODY, "heart_charm")));
        ATTACHMENTS.put(ModItems.DIAMOND_CHARM, List.of(worn(BodyPart.BODY, "diamond_charm")));
        ATTACHMENTS.put(ModItems.CLUB_CHARM, List.of(worn(BodyPart.BODY, "club_charm")));
        ATTACHMENTS.put(ModItems.SPADE_CHARM, List.of(worn(BodyPart.BODY, "spade_charm")));
    }

    /** Focusing Gloves are held, not worn: while held, one glove is drawn on each arm (see GlovesRenderer). */
    public static final List<Attachment> GLOVES = List.of(
            worn(BodyPart.RIGHT_ARM, "focusing_gloves_right"),
            worn(BodyPart.LEFT_ARM, "focusing_gloves_left"));

    private WornModels() {
    }

    private static Attachment worn(BodyPart part, String name) {
        return new Attachment(part, ArcaneArtifacts.id("item/worn/" + name), Placement.NONE);
    }

    private static Attachment turned(BodyPart part, String name, float pivotX, float pivotY, float pivotZ, float rotX, float rotY, float rotZ) {
        return new Attachment(part, ArcaneArtifacts.id("item/worn/" + name),
                new Placement(pivotX, pivotY, pivotZ, rotX, rotY, rotZ, 0f, 0f, 0f, false, 0f));
    }

    public static Map<Supplier<? extends Item>, List<Attachment>> all() {
        return ATTACHMENTS;
    }
}
