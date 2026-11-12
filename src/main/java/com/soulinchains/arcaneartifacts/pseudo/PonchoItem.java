package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Pseudomage's Poncho (back slot). Stand still and the duel begins: every 2 seconds the wearer's spells
 * gain one Amplify (Duel Focus, up to 5), while a whistled western tune plays. Moving ends the duel.
 */
public class PonchoItem extends ArcaneAccessoryItem {
    public static final int MAX_FOCUS = 5;
    public static final int TICKS_PER_FOCUS = 40;
    private static final String STILL = "arcane_artifacts.poncho_still";
    private static final String LAST_X = "arcane_artifacts.poncho_x";
    private static final String LAST_Z = "arcane_artifacts.poncho_z";

    /** An original whistled showdown tune: {tick, note-block semitone (0 = F#3), instrument}. */
    private static final int FLUTE = 0, GUITAR = 1, BELL = 2, BASS = 3;
    private static final int[][] TUNE = {
            {0, 15, FLUTE}, {6, 22, FLUTE}, {12, 15, FLUTE}, {18, 22, FLUTE}, {24, 15, FLUTE},
            {36, 3, GUITAR}, {40, 10, GUITAR}, {44, 3, BASS},
            {48, 22, FLUTE}, {54, 20, FLUTE}, {60, 18, FLUTE}, {66, 15, FLUTE}, {72, 13, FLUTE}, {78, 10, FLUTE},
            {88, 15, BELL}, {92, 3, BASS},
            {96, 15, FLUTE}, {102, 22, FLUTE}, {108, 15, FLUTE}, {114, 22, FLUTE}, {120, 18, FLUTE}, {126, 20, FLUTE},
            {132, 15, FLUTE}, {144, 3, GUITAR}, {148, 3, GUITAR}, {152, 10, GUITAR}, {156, 3, BASS},
            {168, 22, BELL}, {176, 15, BELL}
    };
    public static final int TUNE_LENGTH = 192;

    public PonchoItem() {
        super(new Item.Properties().rarity(Rarity.RARE), 3);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (slotContext.cosmetic() || !(wearer instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        var data = player.getPersistentData();
        Vec3 pos = player.position();
        double dx = pos.x - data.getDouble(LAST_X);
        double dz = pos.z - data.getDouble(LAST_Z);
        data.putDouble(LAST_X, pos.x);
        data.putDouble(LAST_Z, pos.z);
        boolean still = dx * dx + dz * dz < 0.0004 && player.onGround();
        int ticks = still ? data.getInt(STILL) + 1 : 0;
        data.putInt(STILL, ticks);
        if (!still) {
            return;
        }
        int focus = Math.min(MAX_FOCUS, ticks / TICKS_PER_FOCUS);
        if (focus > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.DUEL_FOCUS, 10, focus - 1, true, true, true));
            if (ticks % TICKS_PER_FOCUS == 0 && focus <= MAX_FOCUS && ticks / TICKS_PER_FOCUS <= MAX_FOCUS) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(), 6 + focus * 2, 0.4, 0.5, 0.4, 0.1);
            }
        }
        if (ticks >= 20) {
            playTune(level, player, (ticks - 20) % TUNE_LENGTH);
        }
    }

    private static void playTune(ServerLevel level, Player player, int tick) {
        for (int[] note : TUNE) {
            if (note[0] == tick) {
                float pitch = (float) Math.pow(2.0, (note[1] - 12) / 12.0);
                SoundEvent sound = switch (note[2]) {
                    case GUITAR -> SoundEvents.NOTE_BLOCK_GUITAR.value();
                    case BELL -> SoundEvents.NOTE_BLOCK_BELL.value();
                    case BASS -> SoundEvents.NOTE_BLOCK_BASS.value();
                    default -> SoundEvents.NOTE_BLOCK_FLUTE.value();
                };
                level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.RECORDS, note[2] == FLUTE ? 0.7f : 0.5f, pitch);
            }
        }
    }
}
