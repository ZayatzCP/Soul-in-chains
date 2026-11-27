package com.soulinchains.arcaneartifacts.geo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a stone wing, built like a bird's. A bone of three parts - upper arm, forearm, hand - runs out from the
 * shoulder, bent forward at the elbow and back at the wrist. Feathers grow from it: short tertials at the shoulder, the
 * secondaries along the forearm pointing back, the primaries fanning out from the hand to the tip, a row of greater coverts
 * over their roots, lesser coverts along the leading edge and the little alula at the wrist. The wing beats slowly about
 * the shoulder and half folds on the upstroke, as a bird's does.
 */
public final class BirdWing {
    /** One feather: where its root is, which way it points, which way its face looks, and its size. */
    public record Feather(Vec3 root, Vec3 dir, Vec3 normal, float length, float width, float thick) {
    }

    /** Feathers per wing, in this order: tertials, secondaries, primaries, greater coverts, lesser coverts, alula. */
    public static final int TERTIALS = 3;
    public static final int SECONDARIES = 9;
    public static final int PRIMARIES = 8;
    public static final int GREATER = 9;
    public static final int LESSER = 6;
    public static final int COUNT = TERTIALS + SECONDARIES + PRIMARIES + GREATER + LESSER + 1;

    private BirdWing() {
    }

    /** The beat at a moment: how high the wing is lifted (radians) and how far it is folded (0 to 1). */
    static double lift(double time, boolean flying) {
        return 0.15 + Math.sin(time * 0.16) * (flying ? 0.45 : 0.22);
    }

    static double fold(double time, boolean flying) {
        // folded most while rising, open on the downstroke
        return Math.max(0, Math.cos(time * 0.16)) * (flying ? 0.45 : 0.2);
    }

    /** The feathers of one wing ({@code side} +1 right, -1 left) of a player now. */
    public static List<Feather> wing(Player player, int side) {
        return wing(player.position(), player.yBodyRot, player.level().getGameTime(), player.getAbilities().flying, side);
    }

    public static List<Feather> wing(Vec3 at, float bodyYaw, double time, boolean flying, int side) {
        double yaw = Math.toRadians(bodyYaw);
        Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = new Vec3(0, 1, 0);
        double lift = lift(time, flying);
        double fold = fold(time, flying);
        // the wing's plane: out along the span, the normal upwards, back
        Vec3 out = right.scale(side * Math.cos(lift)).add(up.scale(Math.sin(lift)));
        Vec3 normal = right.scale(-side * Math.sin(lift)).add(up.scale(Math.cos(lift)));
        Vec3 shoulder = at.add(0, 1.45, 0).add(back.scale(0.2)).add(right.scale(side * 0.3));
        // the bone, in the plane: (along the span, back)
        double[] elbow = {1.5, 0.3 + fold * 0.3};
        double[] wrist = {3.6 - fold * 0.9, -0.15 + fold * 0.9};
        double[] tip = {5.6 - fold * 1.8, 0.5 + fold * 2.0};
        List<Feather> out1 = new ArrayList<>(COUNT);
        // tertials: short, at the shoulder, straight back
        for (int i = 0; i < TERTIALS; i++) {
            double t = (i + 1.0) / (TERTIALS + 1);
            out1.add(feather(shoulder, out, back, normal, lerp(new double[]{0.3, 0.1}, elbow, t), -4 + 3 * i, 1.7F + 0.2F * i, 0.6F, 0.12F, 0));
        }
        // secondaries: along the forearm, pointing back, a little outwards
        for (int i = 0; i < SECONDARIES; i++) {
            double t = i / (SECONDARIES - 1.0);
            out1.add(feather(shoulder, out, back, normal, lerp(elbow, wrist, t), 4 + 16 * t, 2.5F + 0.4F * (float) t, 0.6F, 0.12F, 0));
        }
        // primaries: a fan from the hand, the outer ones pointing along the span, the longest near the tip
        for (int i = 0; i < PRIMARIES; i++) {
            double t = i / (PRIMARIES - 1.0);
            float length = (float) (3.0 + 1.6 * Math.sin(Math.PI * 0.85 * t));
            double angle = 24 + 58 * t - fold * 30 * t;
            out1.add(feather(shoulder, out, back, normal, lerp(wrist, tip, t * 0.92), angle, length, 0.5F, 0.12F, 0));
        }
        // greater coverts: over the roots of the flight feathers, shorter, a little above
        for (int i = 0; i < GREATER; i++) {
            double t = i / (GREATER - 1.0);
            double[] root = t < 0.55 ? lerp(elbow, wrist, t / 0.55) : lerp(wrist, tip, (t - 0.55) / 0.45 * 0.7);
            double angle = t < 0.55 ? 8 + 14 * t / 0.55 : 30 + 40 * (t - 0.55) / 0.45;
            out1.add(feather(shoulder, out, back, normal, root, angle, 1.25F, 0.7F, 0.14F, 0.13));
        }
        // lesser coverts: the leading edge, over the bone
        for (int i = 0; i < LESSER; i++) {
            double t = i / (LESSER - 1.0);
            double[] root = t < 0.5 ? lerp(new double[]{0.2, -0.15}, elbow, t / 0.5) : lerp(elbow, wrist, (t - 0.5) / 0.5);
            out1.add(feather(shoulder, out, back, normal, new double[]{root[0], root[1] - 0.35}, 10, 0.9F, 0.75F, 0.16F, 0.24));
        }
        // the alula: a small feather at the wrist, pointing forward and out
        out1.add(feather(shoulder, out, back, normal, wrist, 125, 0.9F, 0.35F, 0.12F, 0.2));
        return out1;
    }

    private static double[] lerp(double[] a, double[] b, double t) {
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
    }

    /**
     * A feather rooted at (along, back) in the wing's plane, pointing {@code angle} degrees from straight back towards the
     * span, drooping a touch, raised {@code above} off the plane (coverts lie over the flight feathers).
     */
    private static Feather feather(Vec3 shoulder, Vec3 out, Vec3 back, Vec3 normal, double[] at, double angle, float length, float width, float thick,
                                   double above) {
        Vec3 root = shoulder.add(out.scale(at[0])).add(back.scale(at[1])).add(normal.scale(above));
        double a = Math.toRadians(angle);
        Vec3 dir = back.scale(Math.cos(a)).add(out.scale(Math.sin(a))).subtract(normal.scale(0.12)).normalize();
        Vec3 face = normal.subtract(dir.scale(normal.dot(dir))).normalize();
        return new Feather(root, dir, face, length, width, thick);
    }
}
