package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

/**
 * Cattos have a life of their own: now and then two idle Cattos close to each other have a little conversation (three
 * lines over their heads), and a Catto on its own may hum a well-known tune, played note by note on a flute.
 * The tunes are old public domain melodies.
 */
public final class CattoChatter {
    public static final int TOPICS = 10;

    /** Melodies in note block pitches (0 = F#3, 12 = F#4) with the length of each note in ticks. */
    public enum Song {
        ODE_TO_JOY(new int[]{10, 10, 11, 13, 13, 11, 10, 8, 6, 6, 8, 10, 10, 8, 8},
                new int[]{6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 9, 3, 12}),
        TWINKLE(new int[]{6, 6, 13, 13, 15, 15, 13, 11, 11, 10, 10, 8, 8, 6},
                new int[]{6, 6, 6, 6, 6, 6, 12, 6, 6, 6, 6, 6, 6, 12}),
        FRERE_JACQUES(new int[]{6, 8, 10, 6, 6, 8, 10, 6, 10, 11, 13, 10, 11, 13},
                new int[]{6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 12, 6, 6, 12}),
        KOROBEINIKI(new int[]{10, 5, 6, 8, 6, 5, 3, 3, 6, 10, 8, 6, 5, 6, 8, 10, 6, 3, 3},
                new int[]{8, 4, 4, 8, 4, 4, 8, 4, 4, 8, 4, 4, 12, 4, 8, 8, 8, 8, 12}),
        FUR_ELISE(new int[]{10, 9, 10, 9, 10, 5, 8, 6, 3},
                new int[]{4, 4, 4, 4, 4, 4, 4, 4, 12});

        final int[] notes;
        final int[] lengths;

        Song(int[] notes, int[] lengths) {
            this.notes = notes;
            this.lengths = lengths;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private record Line(long due, Catto speaker, Component text) {
    }

    private final Catto cat;
    private int cooldown;
    private final ArrayDeque<Line> lines = new ArrayDeque<>();
    private final ArrayDeque<long[]> notes = new ArrayDeque<>();

    CattoChatter(Catto cat) {
        this.cat = cat;
        this.cooldown = 600 + cat.getRandom().nextInt(2400);
    }

    /** Idle, not in a conversation, not saying anything: free to chat. */
    public boolean free() {
        CattoBrain.State state = cat.brain().state();
        boolean idle = cat.getOwnerUUID() == null || state == CattoBrain.State.IDLE || state == CattoBrain.State.WAIT
                || state == CattoBrain.State.OFFLINE;
        return idle && lines.isEmpty() && notes.isEmpty() && cat.speech().getString().isEmpty();
    }

    void tick(ServerLevel level) {
        long now = level.getGameTime();
        while (!notes.isEmpty() && notes.peek()[0] <= now) {
            int note = (int) notes.poll()[1];
            level.playSound(null, cat.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 0.7F,
                    (float) Math.pow(2.0, (note - 12) / 12.0));
            level.sendParticles(ParticleTypes.NOTE, cat.getX(), cat.getY() + 1.1, cat.getZ(), 0, note / 24.0, 0.0, 0.0, 1.0);
        }
        while (!lines.isEmpty() && lines.peek().due() <= now) {
            Line line = lines.poll();
            if (line.speaker().isAlive()) {
                line.speaker().say(line.text(), 70);
                line.speaker().playSound(SoundEvents.CAT_AMBIENT, 0.6F, 1.0F + (line.speaker().getRandom().nextFloat() - 0.5F) * 0.3F);
            }
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = 1800 + cat.getRandom().nextInt(3600);
        if (!free()) {
            return;
        }
        List<Catto> others = level.getEntitiesOfClass(Catto.class, cat.getBoundingBox().inflate(8), c -> c != cat && c.isAlive() && c.chatter.free());
        if (!others.isEmpty() && cat.getRandom().nextFloat() < 0.75F) {
            converse(others.get(0), now);
        } else if (cat.getRandom().nextFloat() < 0.5F) {
            sing(now);
        }
    }

    /** Three lines, each Catto in its own temper. */
    private void converse(Catto other, long now) {
        int topic = cat.getRandom().nextInt(TOPICS);
        String base = "arcane_artifacts.catto.chat." + topic;
        cat.lookAt(other, 30.0F, 30.0F);
        other.lookAt(cat, 30.0F, 30.0F);
        lines.add(new Line(now, cat, Component.translatable(base + ".a." + cat.nature().key())));
        lines.add(new Line(now + 50, other, Component.translatable(base + ".b." + other.nature().key())));
        lines.add(new Line(now + 100, cat, Component.translatable(base + ".c")));
    }

    public void sing(long now) {
        Song[] songs = Song.values();
        Song song = songs[cat.getRandom().nextInt(songs.length)];
        cat.say(Component.translatable("arcane_artifacts.catto.song.humming", Component.translatable("arcane_artifacts.catto.song." + song.key())), 120);
        long at = now + 10;
        for (int i = 0; i < song.notes.length; i++) {
            notes.add(new long[]{at, song.notes[i]});
            at += song.lengths[i];
        }
    }
}
