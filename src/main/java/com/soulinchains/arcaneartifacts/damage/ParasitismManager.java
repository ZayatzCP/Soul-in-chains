package com.soulinchains.arcaneartifacts.damage;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.glyph.EffectParasitism;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import com.soulinchains.arcaneartifacts.registry.ModDamageTypes;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side owner of every parasitic link.
 *
 * <h2>Redirection</h2>
 * {@link LivingIncomingDamageEvent} is fired by NeoForge at the very beginning of
 * {@code LivingEntity#hurt}, before shields, invulnerability frames, armor, enchantments, resistance
 * and absorption are applied to the victim. Cancelling it there and calling {@code host.hurt(source,
 * amount)} therefore moves the full, unreduced damage onto the host, whose own armor and resistances
 * then apply. The caster takes nothing.
 *
 * <h2>Safety rules</h2>
 * <ul>
 *     <li>Recursion guard: while a redirect is in progress no other redirect can happen, so redirected
 *     damage is never redirected again (no chains, no A&lt;-&gt;B ping-pong, no infinite loops).</li>
 *     <li>Damage types in {@code arcane_artifacts:not_redirectable} and every type that bypasses
 *     invulnerability (void, /kill) always reach the caster.</li>
 *     <li>If the host is immune to the original damage type (fire on a fire-immune mob, drowning on a
 *     water mob...), the damage is converted to {@code arcane_artifacts:parasitic_transfer}: still
 *     reduced by the host's armor, credited to the original attacker. The caster stays protected and an
 *     immune host cannot be used as a free shield.</li>
 *     <li>The link breaks when the host dies or is removed, the caster dies/logs out/changes dimension,
 *     the distance exceeds the radius, or the duration ends. A broken link redirects nothing, so damage
 *     simply reaches the caster normally.</li>
 *     <li>One link per caster; a 20 s cooldown starts when a link is created. A second hit while linked
 *     (e.g. double-cast with the Focusing Gloves) neither creates a second link nor extends the first.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class ParasitismManager {

    /** caster UUID -> link. Only touched from the server thread. */
    private static final Map<UUID, Link> LINKS = new ConcurrentHashMap<>();
    private static boolean redirecting;

    private record Link(UUID caster, UUID host, ResourceKey<Level> dimension, long expiresAt,
                        com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData style) {
    }

    private ParasitismManager() {
    }

    public static boolean hasLink(LivingEntity caster) {
        return LINKS.containsKey(caster.getUUID());
    }

    public static long cooldownRemaining(LivingEntity caster) {
        return Math.max(0, caster.getData(ModAttachments.PARASITISM_COOLDOWN_UNTIL) - caster.level().getGameTime());
    }

    /** Creates a link from {@code caster} to {@code host} if every rule allows it. */
    public static boolean tryLink(ServerLevel level, LivingEntity caster, LivingEntity host) {
        return tryLink(level, caster, host, com.soulinchains.arcaneartifacts.fx.Fx.defaults(com.soulinchains.arcaneartifacts.fx.ModTimelines.PARASITISM).main);
    }

    /** Same, with the tether drawn in the given Ars particle style (the casting spell's Parasitism timeline). */
    public static boolean tryLink(ServerLevel level, LivingEntity caster, LivingEntity host,
                                  com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData style) {
        if (host == caster || !host.isAlive() || !caster.isAlive() || host instanceof ArmorStand || host.level() != caster.level()) {
            return false;
        }
        if (hasLink(caster) || cooldownRemaining(caster) > 0) {
            // Covers the second projectile of a Focusing Gloves double-cast: no new link, no extension.
            return false;
        }
        double radius = Config.PARASITISM_RADIUS.get();
        if (host.distanceToSqr(caster) > radius * radius) {
            return false;
        }
        long now = level.getGameTime();
        int duration = Config.PARASITISM_DURATION_TICKS.get();
        int cooldown = Config.PARASITISM_COOLDOWN_TICKS.get();
        LINKS.put(caster.getUUID(), new Link(caster.getUUID(), host.getUUID(), level.dimension(), now + duration, style));
        caster.setData(ModAttachments.PARASITISM_COOLDOWN_UNTIL, now + cooldown);

        host.addEffect(new MobEffectInstance(ModEffects.PARASITE_HOST, duration, 0, false, true, true), caster);
        caster.addEffect(new MobEffectInstance(ModEffects.PARASITIC_BOND, duration, 0, false, false, true));
        if (cooldown > 0) {
            caster.addEffect(new MobEffectInstance(ModEffects.PARASITIC_RECOVERY, cooldown, 0, false, false, true));
        }
        level.playSound(null, host.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    /** Refuses Parasitism casts during the cooldown before any mana is spent. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpellCast(SpellCastEvent event) {
        LivingEntity caster = event.getEntity();
        if (caster.level().isClientSide || event.spell == null) {
            return;
        }
        boolean hasParasitism = false;
        for (AbstractSpellPart part : event.spell.recipe()) {
            if (part == EffectParasitism.INSTANCE) {
                hasParasitism = true;
                break;
            }
        }
        if (!hasParasitism) {
            return;
        }
        long remaining = cooldownRemaining(caster);
        if (remaining > 0) {
            event.setCanceled(true);
            if (caster instanceof Player player) {
                player.displayClientMessage(Component.translatable("arcane_artifacts.parasitism.cooldown",
                        String.format("%.1f", remaining / 20.0)).withStyle(ChatFormatting.RED), true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (redirecting || victim.level().isClientSide || LINKS.isEmpty() || event.getAmount() <= 0) {
            return;
        }
        Link link = LINKS.get(victim.getUUID());
        if (link == null) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(ModTags.NOT_REDIRECTABLE) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        LivingEntity host = resolveHost(victim, link);
        if (host == null) {
            breakLink(victim.getServer(), link);
            return;
        }
        float amount = event.getAmount();
        event.setCanceled(true);
        DamageSource redirected = isImmune(host, source)
                ? ModDamageTypes.parasiticTransfer(host.level(), source.getDirectEntity(), source.getEntity())
                : source;
        redirecting = true;
        try {
            host.hurt(redirected, amount);
        } finally {
            redirecting = false;
        }
        if (host.level() instanceof ServerLevel level && level.getGameTime() % 4 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, host.getX(), host.getY(0.6), host.getZ(), 3, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private static boolean isImmune(LivingEntity host, DamageSource source) {
        if (host.isInvulnerableTo(source)) return true;
        if (source.is(DamageTypeTags.IS_FIRE) && (host.fireImmune() || host.hasEffect(MobEffects.FIRE_RESISTANCE))) return true;
        if (source.is(DamageTypeTags.IS_DROWNING) && host.canBreatheUnderwater()) return true;
        return source.is(DamageTypeTags.IS_FALL) && host.getType().is(net.minecraft.tags.EntityTypeTags.FALL_DAMAGE_IMMUNE);
    }

    private static LivingEntity resolveHost(LivingEntity caster, Link link) {
        if (!(caster.level() instanceof ServerLevel level) || !level.dimension().equals(link.dimension())) {
            return null;
        }
        Entity entity = level.getEntity(link.host());
        if (!(entity instanceof LivingEntity host) || !host.isAlive() || host.isRemoved()) {
            return null;
        }
        double radius = Config.PARASITISM_RADIUS.get();
        if (host.distanceToSqr(caster) > radius * radius || level.getGameTime() >= link.expiresAt()) {
            return null;
        }
        return host;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (LINKS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Link> it = LINKS.values().iterator();
        while (it.hasNext()) {
            Link link = it.next();
            ServerLevel level = server.getLevel(link.dimension());
            Entity casterEntity = level == null ? null : level.getEntity(link.caster());
            if (!(casterEntity instanceof LivingEntity caster) || !caster.isAlive()) {
                it.remove();
                cleanupEffects(server, link, null, level);
                continue;
            }
            LivingEntity host = resolveHost(caster, link);
            if (host == null) {
                it.remove();
                cleanupEffects(server, link, caster, level);
                if (caster instanceof Player player) {
                    player.displayClientMessage(Component.translatable("arcane_artifacts.parasitism.broken").withStyle(ChatFormatting.GRAY), true);
                }
                continue;
            }
            if (level.getGameTime() % 4 == 0) {
                drawLink(level, caster, host, link.style());
            }
        }
    }

    private static void breakLink(MinecraftServer server, Link link) {
        if (LINKS.remove(link.caster()) != null && server != null) {
            ServerLevel level = server.getLevel(link.dimension());
            Entity caster = level == null ? null : level.getEntity(link.caster());
            cleanupEffects(server, link, caster instanceof LivingEntity living ? living : null, level);
        }
    }

    private static void cleanupEffects(MinecraftServer server, Link link, LivingEntity caster, ServerLevel level) {
        if (caster != null) {
            caster.removeEffect(ModEffects.PARASITIC_BOND);
        }
        if (level != null && level.getEntity(link.host()) instanceof LivingEntity host) {
            host.removeEffect(ModEffects.PARASITE_HOST);
        }
    }

    /** A tether between caster and host, drawn with the spell's Ars particle style and sent to nearby players. */
    private static void drawLink(ServerLevel level, LivingEntity caster, LivingEntity host,
                                 com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData style) {
        Vec3 from = caster.position().add(0, caster.getBbHeight() * 0.6, 0);
        Vec3 to = host.position().add(0, host.getBbHeight() * 0.6, 0);
        com.soulinchains.arcaneartifacts.fx.Fx.line(level, style, from, to);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LINKS.clear();
        redirecting = false;
    }
}
