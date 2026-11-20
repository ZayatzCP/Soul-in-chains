package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Souls: what a dying mob leaves and how the Shackled catches it.
 * <ul>
 *     <li>A soul appears only when a player (not a fake player or a machine) killed the mob, like experience.</li>
 *     <li>Chance {@code soul_chance}; always with the Chain Sickle or under Reap.</li>
 *     <li>Reap puts the soul straight into the reaper's lantern (one step up with Amplify).</li>
 *     <li>Otherwise a soul wisp floats over the body for a few seconds; a Soul Lantern nearby draws it in.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class Souls {
    /** Persistent data on a creature marked by Reap. */
    public static final String REAPER_TAG = ArcaneArtifacts.MODID + ":reaper";

    private Souls() {
    }

    public static Optional<ItemStack> lantern(LivingEntity entity) {
        return CurioHelper.findEquipped(entity, ModItems.SOUL_LANTERN.get());
    }

    public static int count(LivingEntity entity, SoulType type) {
        return lantern(entity).map(s -> SoulLanternItem.store(s).count(type)).orElse(0);
    }

    public static int total(LivingEntity entity) {
        return lantern(entity).map(s -> SoulLanternItem.store(s).total()).orElse(0);
    }

    /** Puts a soul into the entity's lantern. Returns false when it has no lantern or the lantern is full. */
    public static boolean store(LivingEntity entity, SoulType type) {
        Optional<ItemStack> found = lantern(entity);
        if (found.isEmpty()) {
            return false;
        }
        ItemStack lantern = found.get();
        SoulStore store = SoulLanternItem.store(lantern);
        if (store.total() >= SoulLanternItem.capacity(lantern)) {
            return false;
        }
        lantern.set(ModComponents.SOUL_STORE, store.with(type, 1));
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    /** Takes a soul out of the entity's lantern. Returns false when there is none of that kind. */
    public static boolean take(LivingEntity entity, SoulType type) {
        Optional<ItemStack> found = lantern(entity);
        if (found.isEmpty()) {
            return false;
        }
        SoulStore store = SoulLanternItem.store(found.get());
        if (store.count(type) <= 0) {
            return false;
        }
        found.get().set(ModComponents.SOUL_STORE, store.with(type, -1));
        return true;
    }

    /** Spends a soul (raising, devouring, rituals): it leaves the lantern and adds Corruption. */
    public static boolean spend(Player player, SoulType type) {
        if (!take(player, type)) {
            return false;
        }
        if (!Relics.toothNecklaceSpares(player)) {
            Corruption.spend(player, type);
        }
        return true;
    }

    /** A soul caught from the world (wisp, Reap): the full Shackled set sometimes makes it stronger. */
    public static boolean catchSoul(LivingEntity catcher, SoulType type) {
        return store(catcher, ShackledSet.caught(catcher, type));
    }

    public static boolean canStore(LivingEntity entity) {
        return lantern(entity).map(s -> SoulLanternItem.store(s).total() < SoulLanternItem.capacity(s)).orElse(false);
    }

    /** Reap: the next death of this creature goes to the reaper. */
    public static void markForReaping(LivingEntity target, UUID reaper, boolean upgrade) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("by", reaper);
        tag.putBoolean("up", upgrade);
        tag.putLong("until", target.level().getGameTime() + 20L * 60);
        target.getPersistentData().put(REAPER_TAG, tag);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || dead instanceof Player) {
            return;
        }
        SoulType type = SoulType.of(dead);
        if (type == null) {
            return;
        }
        // Reap: straight into the reaper's lantern, whoever dealt the last blow.
        if (dead.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.REAP_MARK) && dead.getPersistentData().contains(REAPER_TAG)) {
            CompoundTag tag = dead.getPersistentData().getCompound(REAPER_TAG);
            SoulType reaped = tag.getBoolean("up") ? type.upgraded() : type;
            if (tag.hasUUID("by") && level.getEntity(tag.getUUID("by")) instanceof LivingEntity reaper && catchSoul(reaper, reaped)) {
                SoulWisp.burst(level, dead.position().add(0, dead.getBbHeight() / 2, 0), reaped);
                return;
            }
            spawnWisp(level, dead, reaped);
            return;
        }
        Player killer = killer(event.getSource());
        if (killer == null) {
            return;
        }
        // Executed with the Executioner's Axe: always a soul, one step stronger.
        if (dead.getTags().contains(ExecutionerAxeItem.EXECUTED_TAG)) {
            spawnWisp(level, dead, type.upgraded());
            return;
        }
        var weapon = event.getSource().getWeaponItem();
        boolean dagger = weapon != null && weapon.getItem() instanceof RitualDaggerItem;
        boolean sure = weapon != null && weapon.getItem() instanceof ChainSickleItem || dagger;
        if (dagger && dead.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.SHACKLED)) {
            // A proper ritual kill: the chained creature's soul comes clean and washes a little Corruption away.
            Corruption.add(killer, -RitualDaggerItem.CORRUPTION_WASH);
        }
        if (sure || level.random.nextDouble() < Config.SOUL_CHANCE.get()) {
            spawnWisp(level, dead, type);
        }
    }

    @Nullable
    private static Player killer(DamageSource source) {
        return source.getEntity() instanceof Player player && !(player instanceof FakePlayer) ? player : null;
    }

    public static void spawnWisp(ServerLevel level, LivingEntity dead, SoulType type) {
        SoulWisp wisp = new SoulWisp(ModEntities.SOUL_WISP.get(), level);
        wisp.setSoul(type);
        wisp.setPos(dead.getX(), dead.getY() + dead.getBbHeight() * 0.6, dead.getZ());
        level.addFreshEntity(wisp);
    }
}
