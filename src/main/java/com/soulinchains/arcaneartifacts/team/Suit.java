package com.soulinchains.arcaneartifacts.team;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.function.Supplier;

/** The four suits of the sworn company and the gifts each one gives its bearer once sworn. */
public enum Suit {
    HEARTS("hearts", ChatFormatting.RED, List.of(
            new Bonus(() -> Attributes.MAX_HEALTH, 6.0, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(() -> Attributes.KNOCKBACK_RESISTANCE, 0.2, AttributeModifier.Operation.ADD_VALUE))),
    DIAMONDS("diamonds", ChatFormatting.GOLD, List.of(
            new Bonus(() -> Attributes.LUCK, 3.0, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(() -> Attributes.MOVEMENT_SPEED, 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))),
    CLUBS("clubs", ChatFormatting.DARK_GREEN, List.of(
            new Bonus(() -> Attributes.ATTACK_DAMAGE, 2.5, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(() -> Attributes.ATTACK_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))),
    SPADES("spades", ChatFormatting.DARK_AQUA, List.of(
            new Bonus(() -> Attributes.ARMOR, 4.0, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(() -> Attributes.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_VALUE)));

    /** One attribute gift. */
    public record Bonus(Supplier<Holder<Attribute>> attribute, double amount, AttributeModifier.Operation operation) {
    }

    private final String id;
    private final ChatFormatting color;
    private final List<Bonus> bonuses;

    Suit(String id, ChatFormatting color, List<Bonus> bonuses) {
        this.id = id;
        this.color = color;
        this.bonuses = bonuses;
    }

    public String id() {
        return id;
    }

    public ChatFormatting color() {
        return color;
    }

    public List<Bonus> bonuses() {
        return bonuses;
    }
}
