package com.soulinchains.arcaneartifacts.belt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The three bottomless pouches of the belt. Each pouch holds one kind of item (a template stack of 1)
 * in any amount. A pouch takes the kind of the first item put into it and forgets it when emptied.
 * Immutable: every change returns a new value (data components must not be mutated).
 */
public record BeltContents(List<Pouch> pouches) {
    public static final int POUCHES = 3;

    public record Pouch(ItemStack type, long count) {
        public static final Pouch EMPTY = new Pouch(ItemStack.EMPTY, 0);

        public static final Codec<Pouch> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.OPTIONAL_CODEC.fieldOf("type").forGetter(Pouch::type),
                Codec.LONG.fieldOf("count").forGetter(Pouch::count)
        ).apply(i, Pouch::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Pouch> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Pouch::type,
                ByteBufCodecs.VAR_LONG, Pouch::count,
                Pouch::new);

        public boolean isEmpty() {
            return type.isEmpty() || count <= 0;
        }

        public boolean accepts(ItemStack stack) {
            return !stack.isEmpty() && (isEmpty() || ItemStack.isSameItemSameComponents(type, stack));
        }
    }

    public static final BeltContents EMPTY = new BeltContents(List.of(Pouch.EMPTY, Pouch.EMPTY, Pouch.EMPTY));

    public static final Codec<BeltContents> CODEC = Pouch.CODEC.listOf().xmap(BeltContents::new, BeltContents::pouches);
    public static final StreamCodec<RegistryFriendlyByteBuf, BeltContents> STREAM_CODEC =
            Pouch.STREAM_CODEC.apply(ByteBufCodecs.list()).map(BeltContents::new, BeltContents::pouches);

    public BeltContents {
        List<Pouch> fixed = new ArrayList<>(pouches);
        while (fixed.size() < POUCHES) {
            fixed.add(Pouch.EMPTY);
        }
        pouches = List.copyOf(fixed.subList(0, POUCHES));
    }

    public Pouch get(int index) {
        return pouches.get(index);
    }

    /** The pouch that already holds this kind of item, else the first empty pouch, else -1. */
    public int pouchFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < POUCHES; i++) {
            if (!pouches.get(i).isEmpty() && ItemStack.isSameItemSameComponents(pouches.get(i).type(), stack)) {
                return i;
            }
        }
        return -1;
    }

    /** Only pouches that already hold this kind of item (used for automatic collection). */
    public boolean collects(ItemStack stack) {
        return pouchFor(stack) >= 0;
    }

    public int firstFree() {
        for (int i = 0; i < POUCHES; i++) {
            if (pouches.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public BeltContents add(int index, ItemStack stack, long amount) {
        Pouch pouch = pouches.get(index);
        ItemStack type = pouch.isEmpty() ? stack.copyWithCount(1) : pouch.type();
        return with(index, new Pouch(type, Math.max(0, pouch.isEmpty() ? amount : pouch.count() + amount)));
    }

    public BeltContents remove(int index, long amount) {
        Pouch pouch = pouches.get(index);
        long left = Math.max(0, pouch.count() - amount);
        return with(index, left == 0 ? Pouch.EMPTY : new Pouch(pouch.type(), left));
    }

    private BeltContents with(int index, Pouch pouch) {
        List<Pouch> copy = new ArrayList<>(pouches);
        copy.set(index, pouch);
        return new BeltContents(copy);
    }

    /** "12", "4.5k", "1.2M"... */
    public static String format(long count) {
        if (count < 10_000) return Long.toString(count);
        if (count < 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fk", count / 1000.0);
        if (count < 1_000_000_000) return String.format(java.util.Locale.ROOT, "%.1fM", count / 1_000_000.0);
        return String.format(java.util.Locale.ROOT, "%.1fG", count / 1_000_000_000.0);
    }
}
