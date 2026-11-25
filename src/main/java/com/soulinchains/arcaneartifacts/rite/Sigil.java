package com.soulinchains.arcaneartifacts.rite;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The fifteen chalk sigils. Each is a figure on a 3x3 grid of floor blocks: its nodes are blocks with chalk, its lines join
 * orthogonal neighbours. A figure is the same sigil turned or mirrored. Seven are base sigils (they make up a rite), eight
 * are modifiers (they tune what the rite brings).
 */
public enum Sigil {
    OGRADA("ograda", ChalkColor.WHITE, false, "00-01 01-02 02-12 12-22 22-21 21-20 20-10 10-00"),
    OKO("oko", ChalkColor.WHITE, false, "01-11 11-21 10-11 11-12"),
    PECHAT("pechat", ChalkColor.WHITE, false, "00-01 01-02 02-12 12-22 22-21 21-20 20-10 10-00 01-11 11-21 10-11 11-12"),
    VRATA("vrata", ChalkColor.CINNABAR, false, "20-10 10-00 00-01 01-02 02-12 12-22 01-11"),
    PLAMYA("plamya", ChalkColor.CINNABAR, false, "00-10 10-11 11-12 12-02 01-11 11-21"),
    UZY("uzy", ChalkColor.COAL, false, "02-01 01-00 00-10 10-11 11-12 12-22 22-21 21-20"),
    ZHATVA("zhatva", ChalkColor.COAL, false, "00-10 10-20 20-21 21-22 22-12"),
    SONM("sonm", ChalkColor.CINNABAR, true, "00-01 01-02 00-10 10-20 01-11 11-21 02-12 12-22"),
    VENEC("venec", ChalkColor.WHITE, true, "20-21 21-22 20-10 21-11 11-01 22-12"),
    ROG("rog", ChalkColor.CINNABAR, true, "20-21 21-11 11-12 12-02"),
    VITOK("vitok", ChalkColor.COAL, true, "11-12 12-02 02-01 01-00 00-10 10-20 20-21 21-22"),
    ZHILA("zhila", ChalkColor.CINNABAR, true, "00-01 01-02 01-11 11-21"),
    KLYATVA("klyatva", ChalkColor.WHITE, true, "00-10 10-20 10-11 11-12 02-12 12-22"),
    TISH("tish", ChalkColor.WHITE, true, "10-11 11-12 01-11"),
    KROV("krov", ChalkColor.BONE, true, "00-01 01-11 11-21 21-22");

    public final String id;
    public final ChalkColor color;
    public final boolean modifier;
    /** Lines as cell pairs, each cell {@code row * 3 + column}. */
    public final int[][] lines;
    private static final Map<String, Sigil> BY_SHAPE = new HashMap<>();

    static {
        for (Sigil sigil : values()) {
            BY_SHAPE.put(canonical(sigil.lines), sigil);
        }
    }

    Sigil(String id, ChalkColor color, boolean modifier, String spec) {
        this.id = id;
        this.color = color;
        this.modifier = modifier;
        String[] parts = spec.split(" ");
        lines = new int[parts.length][];
        for (int i = 0; i < parts.length; i++) {
            String a = parts[i].substring(0, 2);
            String b = parts[i].substring(3, 5);
            lines[i] = new int[]{(a.charAt(0) - '0') * 3 + (a.charAt(1) - '0'), (b.charAt(0) - '0') * 3 + (b.charAt(1) - '0')};
        }
    }

    public Component displayName() {
        return Component.translatable("arcane_artifacts.rite.sigil." + id);
    }

    @Nullable
    public static Sigil byId(String id) {
        for (Sigil sigil : values()) {
            if (sigil.id.equals(id)) {
                return sigil;
            }
        }
        return null;
    }

    /** The sigil drawn by these lines (cells 0..8), whatever its turn or mirror, or null. */
    @Nullable
    public static Sigil match(int[][] lines) {
        return lines.length == 0 ? null : BY_SHAPE.get(canonical(lines));
    }

    private static int turn(int cell) {
        int r = cell / 3;
        int c = cell % 3;
        return c * 3 + (2 - r);
    }

    private static int mirror(int cell) {
        return (cell / 3) * 3 + (2 - cell % 3);
    }

    /** The smallest text form of the figure over its eight turns and mirrors. */
    static String canonical(int[][] lines) {
        String best = null;
        for (int m = 0; m < 2; m++) {
            for (int k = 0; k < 4; k++) {
                Set<String> set = new TreeSet<>();
                for (int[] line : lines) {
                    int a = line[0];
                    int b = line[1];
                    if (m == 1) {
                        a = mirror(a);
                        b = mirror(b);
                    }
                    for (int t = 0; t < k; t++) {
                        a = turn(a);
                        b = turn(b);
                    }
                    set.add(Math.min(a, b) + "-" + Math.max(a, b));
                }
                String form = String.join(",", set);
                if (best == null || form.compareTo(best) < 0) {
                    best = form;
                }
            }
        }
        return best;
    }

    /** The base sigils, in order (for the Book). */
    public static List<Sigil> base() {
        List<Sigil> list = new ArrayList<>();
        for (Sigil sigil : values()) {
            if (!sigil.modifier) {
                list.add(sigil);
            }
        }
        return list;
    }
}
