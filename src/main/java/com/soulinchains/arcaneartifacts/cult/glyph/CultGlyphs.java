package com.soulinchains.arcaneartifacts.cult.glyph;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.cult.AshWitness;
import com.soulinchains.arcaneartifacts.cult.Cult;
import com.soulinchains.arcaneartifacts.cult.CultGearEvents;
import com.soulinchains.arcaneartifacts.cult.RitualSealBlockEntity;
import com.soulinchains.arcaneartifacts.cult.RitualServant;
import com.soulinchains.arcaneartifacts.cult.RitualSummons;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** What the Cultist's glyphs share: the Warding Seal zones, the Witness Call, Hold the Circle, Retribution. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class CultGlyphs {
    public static final String INSCRIBED_BY = ArcaneArtifacts.MODID + ":inscribed_by";
    public static final String CALLED_BY = ArcaneArtifacts.MODID + ":called_by";

    public static final double WARD_RADIUS = 3;
    public static final int WARD_TICKS = 80;
    public static final int WARD_COOLDOWN = 160;

    public static final int CALL_LIFE = 600;
    public static final int CALL_COOLDOWN = 100;
    public static final int CALL_CAP = 3;
    public static final float ABUNDANCE_STEP = 0.25F;
    public static final float ABUNDANCE_CAP = 1.5F;

    public static final int HOLD_STABILITY = 10;
    public static final int HOLD_COOLDOWN = 40;

    public static final double COMMAND_RANGE = 24;

    private static final List<Zone> ZONES = new ArrayList<>();
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    private CultGlyphs() {
    }

    // ------------------------------------------------------------------ cooldowns

    /** True (and starts the cooldown) when {@code what} is ready for this caster. */
    public static boolean ready(LivingEntity caster, String what, int ticks) {
        String key = caster.getUUID() + "/" + what;
        long now = caster.level().getGameTime();
        Long until = COOLDOWNS.get(key);
        if (until != null && until > now) {
            return false;
        }
        COOLDOWNS.put(key, now + ticks);
        return true;
    }

    public static void clearCooldowns(LivingEntity caster) {
        COOLDOWNS.keySet().removeIf(k -> k.startsWith(caster.getUUID() + "/"));
    }

    static void say(LivingEntity caster, String key) {
        if (caster instanceof Player player) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.cult." + key), true);
        }
    }

    // ------------------------------------------------------------------ Inscribe

    /** Inscribes the creature for the caster. */
    public static void inscribe(LivingEntity target, LivingEntity caster) {
        CultGearEvents.inscribe(target);
        target.getPersistentData().putUUID(INSCRIBED_BY, caster.getUUID());
    }

    public static boolean inscribedBy(LivingEntity target, UUID caster) {
        return target.hasEffect(ModEffects.INSCRIBED) && target.getPersistentData().hasUUID(INSCRIBED_BY)
                && target.getPersistentData().getUUID(INSCRIBED_BY).equals(caster);
    }

    // ------------------------------------------------------------------ Hold the Circle

    /** Hold the Circle: the caster stands inside the circle of a rite they hold; its anchors gain 10 stability per Hold. */
    public static int hold(LivingEntity caster, int holds) {
        if (holds <= 0 || !(caster instanceof Player player)) {
            return 0;
        }
        RitualSealBlockEntity seal = RitualSealBlockEntity.ofConfessor(player);
        if (seal == null) {
            seal = RitualSealBlockEntity.ofWarden(player);
        }
        if (seal == null || !seal.inside(player) || !ready(player, "hold", HOLD_COOLDOWN)) {
            return 0;
        }
        return seal.mendAnchors(HOLD_STABILITY * holds);
    }

    // ------------------------------------------------------------------ Warding Seal

    record Zone(ResourceKey<Level> dimension, Vec3 at, double radius, UUID caster, @Nullable UUID partner, long until) {
    }

    /** Lays a Warding Seal; false while it is on cooldown for this caster. */
    public static boolean lay(ServerLevel level, Vec3 at, LivingEntity caster, int aoe, int extend) {
        if (!ready(caster, "ward", WARD_COOLDOWN)) {
            say(caster, "ward_cooldown");
            return false;
        }
        UUID partner = caster instanceof Player p ? Cult.get(p).partner().orElse(null) : null;
        ZONES.add(new Zone(level.dimension(), at, WARD_RADIUS + aoe, caster.getUUID(), partner, level.getGameTime() + WARD_TICKS + 40L * extend));
        level.playSound(null, at.x, at.y, at.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8F, 1.4F);
        wardTick(level);
        return true;
    }

    /** Whether a Warding Seal of this caster covers this creature. */
    static boolean covered(Zone zone, LivingEntity e) {
        UUID id = e.getUUID();
        return id.equals(zone.caster) || id.equals(zone.partner)
                || e instanceof RitualServant servant && (servant.servesPair(zone.caster) || zone.partner != null && servant.servesPair(zone.partner));
    }

    /** Wards everyone the seals cover; returns how many were warded. */
    public static int wardTick(ServerLevel level) {
        int warded = 0;
        long now = level.getGameTime();
        Iterator<Zone> it = ZONES.iterator();
        while (it.hasNext()) {
            Zone zone = it.next();
            if (zone.until < now) {
                it.remove();
                continue;
            }
            if (!zone.dimension.equals(level.dimension())) {
                continue;
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(zone.at, zone.at).inflate(zone.radius),
                    e -> e.isAlive() && e.position().distanceTo(zone.at) <= zone.radius + 0.5 && covered(zone, e))) {
                e.addEffect(new MobEffectInstance(ModEffects.WARDED, 20, 0, false, false, true));
                warded++;
            }
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                level.sendParticles(new DustParticleOptions(new Vector3f(0.94F, 0.91F, 0.87F), 1.0F),
                        zone.at.x + Math.cos(a) * zone.radius, zone.at.y, zone.at.z + Math.sin(a) * zone.radius, 1, 0, 0.05, 0, 0);
            }
        }
        return warded;
    }

    public static void clearZones() {
        ZONES.clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ZONES.isEmpty() || event.getServer().getTickCount() % 10 != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            wardTick(level);
        }
    }

    // ------------------------------------------------------------------ Witness Call

    /** The partner the call needs: online, within the link range (creative casters may call alone). */
    @Nullable
    public static Player partnerInRange(Player caster) {
        UUID partner = Cult.get(caster).partner().orElse(null);
        Player found = partner == null ? null : caster.level().getPlayerByUUID(partner);
        if (found != null && found.distanceTo(caster) <= Config.CULT_LINK_RANGE.get()) {
            return found;
        }
        return null;
    }

    /**
     * Brings up Ash Witnesses at {@code at} for the pair: one (Amplify: a common one instead of a faint one), or with Census
     * one more faint one per Census instead. They last 30 s (+10 s per Extend Time); Abundance lifts their loot multiplier.
     */
    public static List<RitualServant> call(ServerLevel level, Vec3 at, Player caster, @Nullable Player partner, int amplify, int census,
                                           int abundance, int extend) {
        List<RitualServant> risen = new ArrayList<>();
        int count = 1 + census;
        RitualServant.Grade grade = census == 0 && amplify > 0 ? RitualServant.Grade.COMMON : RitualServant.Grade.FAINT;
        float loot = Math.min(ABUNDANCE_CAP, 1.0F + ABUNDANCE_STEP * abundance);
        for (int i = 0; i < count; i++) {
            AshWitness witness = ModEntities.ASH_WITNESS.get().create(level);
            if (witness == null) {
                continue;
            }
            double angle = Math.PI * 2 * i / count;
            double r = count > 1 ? 1.2 : 0;
            witness.moveTo(at.x + Math.cos(angle) * r, at.y, at.z + Math.sin(angle) * r, caster.getYRot(), 0);
            boolean wardenCalls = Cult.role(caster) == com.soulinchains.arcaneartifacts.cult.CultRole.WARDEN;
            witness.setup(grade, wardenCalls ? partner : caster, wardenCalls ? caster : partner, CALL_LIFE + 200 * extend);
            RitualSummons.mark(witness, loot);
            witness.getPersistentData().putUUID(CALLED_BY, caster.getUUID());
            level.addFreshEntity(witness);
            level.sendParticles(ParticleTypes.ASH, witness.getX(), witness.getY() + 1, witness.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
            risen.add(witness);
        }
        // no more than three called witnesses per caster: the oldest crumble
        List<AshWitness> mine = level.getEntitiesOfClass(AshWitness.class, caster.getBoundingBox().inflate(64), w -> w.isAlive()
                        && w.getPersistentData().hasUUID(CALLED_BY) && w.getPersistentData().getUUID(CALLED_BY).equals(caster.getUUID()))
                .stream().sorted(Comparator.comparingInt(RitualServant::lifeLeft)).toList();
        for (int i = 0; i < mine.size() - CALL_CAP; i++) {
            mine.get(i).discard();
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.8F, 1.3F);
        return risen;
    }

    // ------------------------------------------------------------------ Retribution

    /**
     * Retribution: the pair's rite-summoned creatures within 24 blocks turn on the target. If it is Inscribed they get
     * Strength (I, +1 per Amplify) for 10 s. Abundance: +0.1 loot multiplier each, once. Returns how many obey.
     */
    public static int command(ServerLevel level, LivingEntity caster, LivingEntity target, int amplify, int abundance) {
        if (target == caster || RitualSummons.summoned(target) || target instanceof Player) {
            return 0;
        }
        UUID partner = caster instanceof Player p ? Cult.get(p).partner().orElse(null) : null;
        if (partner != null && target.getUUID().equals(partner)) {
            return 0;
        }
        boolean inscribed = target.hasEffect(ModEffects.INSCRIBED);
        int obeyed = 0;
        for (RitualServant servant : level.getEntitiesOfClass(RitualServant.class, target.getBoundingBox().inflate(COMMAND_RANGE),
                s -> s.isAlive() && (s.servesPair(caster.getUUID()) || partner != null && s.servesPair(partner)))) {
            servant.setTarget(target);
            if (inscribed) {
                servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, Math.max(0, amplify), false, true, true));
            }
            if (abundance > 0) {
                RitualSummons.addLoot(servant, "retribution", 0.1F * abundance, 2.0F);
            }
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, servant.getX(), servant.getY() + servant.getBbHeight() + 0.3, servant.getZ(), 1, 0, 0, 0, 0);
            obeyed++;
        }
        if (obeyed > 0) {
            level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
        return obeyed;
    }
}
