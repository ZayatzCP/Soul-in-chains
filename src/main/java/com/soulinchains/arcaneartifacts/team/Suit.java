package com.soulinchains.arcaneartifacts.team;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.function.Supplier;

/** The four suits of the sworn company and the small gift each one gives its bearer. */
public enum Suit {
    HEARTS("hearts", ChatFormatting.RED, () -> Attributes.MAX_HEALTH, 4.0),
    DIAMONDS("diamonds", ChatFormatting.GOLD, () -> Attributes.LUCK, 2.0),
    CLUBS("clubs", ChatFormatting.DARK_GREEN, () -> Attributes.ATTACK_DAMAGE, 1.5),
    SPADES("spades", ChatFormatting.DARK_AQUA, () -> Attributes.ARMOR, 2.0);

    private final String id;
    private final ChatFormatting color;
    private final Supplier<Holder<Attribute>> attribute;
    private final double amount;

    Suit(String id, ChatFormatting color, Supplier<Holder<Attribute>> attribute, double amount) {
        this.id = id;
        this.color = color;
        this.attribute = attribute;
        this.amount = amount;
    }

    public String id() {
        return id;
    }

    public ChatFormatting color() {
        return color;
    }

    public Holder<Attribute> attribute() {
        return attribute.get();
    }

    public double amount() {
        return amount;
    }
}
