package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ring of Calm (ring slot). After a while without hostile creatures within 16 blocks the wearer gets
 * Speed II and Mana Regeneration II. The moment a hostile creature or a boss comes close, the ring says
 * (with feeling) what kind of trouble it is.
 */
public class RingOfCalmItem extends ArcaneAccessoryItem {
    private static final double RANGE = 16.0;
    /** Per player: when the last hostile was seen, and which creatures the ring has already announced. */
    private static final Map<UUID, Long> LAST_THREAT = new HashMap<>();
    private static final Map<UUID, Set<UUID>> ANNOUNCED = new HashMap<>();
    /** Per player: when each kind of creature was last announced, and when the ring last spoke at all. */
    private static final Map<UUID, Map<EntityType<?>, Long>> LAST_KIND = new HashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE = new HashMap<>();
    private static final long MIN_GAP_TICKS = 200;

    public RingOfCalmItem() {
        super(new Item.Properties().rarity(Rarity.RARE), 2);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer.level().isClientSide || slotContext.cosmetic() || !(wearer instanceof Player player) || player.tickCount % 10 != 0) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        List<Mob> threats = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE),
                mob -> mob instanceof Enemy && mob.isAlive() && mob.distanceToSqr(player) <= RANGE * RANGE);
        Set<UUID> announced = ANNOUNCED.computeIfAbsent(id, k -> new HashSet<>());
        if (threats.isEmpty()) {
            announced.clear();
            long since = LAST_THREAT.computeIfAbsent(id, k -> now);
            if (now - since >= Config.CALM_RING_SECONDS.get() * 20L) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false, true));
                player.addEffect(new MobEffectInstance(ModPotions.MANA_REGEN_EFFECT, 40, 1, true, false, true));
            }
            return;
        }
        LAST_THREAT.put(id, now);
        announced.removeIf(uuid -> threats.stream().noneMatch(mob -> mob.getUUID().equals(uuid)));
        threats.stream()
                .filter(mob -> !announced.contains(mob.getUUID()))
                .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)))
                .ifPresent(mob -> {
                    // The whole pack of that kind counts as announced, said or not.
                    threats.stream().filter(other -> other.getType() == mob.getType()).forEach(other -> announced.add(other.getUUID()));
                    if (!shouldAnnounce(id, mob.getType(), now)) {
                        return;
                    }
                    player.displayClientMessage(Component.translatable("arcane_artifacts.calm_ring.prefix")
                            .append(Component.translatable("arcane_artifacts.calm_ring." + category(mob), mob.getName()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), false);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.7f);
                });
    }

    /**
     * No spam: a kind of creature is announced again only after {@code calm_ring_repeat_seconds}, and
     * the ring never speaks twice within 10 seconds. Records the announcement when it returns true.
     */
    public static boolean shouldAnnounce(UUID player, EntityType<?> kind, long now) {
        long gap = Config.CALM_RING_REPEAT_SECONDS.get() * 20L;
        Map<EntityType<?>, Long> kinds = LAST_KIND.computeIfAbsent(player, k -> new HashMap<>());
        Long lastKind = kinds.get(kind);
        Long lastAny = LAST_MESSAGE.get(player);
        if ((lastKind != null && now - lastKind < gap) || (lastAny != null && now - lastAny < MIN_GAP_TICKS)) {
            return false;
        }
        kinds.put(kind, now);
        LAST_MESSAGE.put(player, now);
        return true;
    }

    /** Which joke the ring makes about a creature. */
    public static String category(Mob mob) {
        EntityType<?> type = mob.getType();
        if (type == EntityType.ENDER_DRAGON) return "dragon";
        if (type == EntityType.WITHER) return "wither";
        if (type == EntityType.WARDEN) return "warden";
        if (type == EntityType.CREEPER) return "explosive";
        if (type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN) return "guardian";
        if (type == EntityType.PHANTOM) return "phantom";
        if (type.is(EntityTypeTags.UNDEAD)) return "undead";
        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) return "spider";
        if (type == EntityType.ENDERMAN || type == EntityType.ENDERMITE) return "ender";
        if (type == EntityType.WITCH) return "witch";
        if (type.is(EntityTypeTags.ILLAGER) || type == EntityType.RAVAGER || type == EntityType.VEX) return "illager";
        if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE) return "slime";
        if (type == EntityType.BLAZE || type == EntityType.GHAST) return "fire";
        if (type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE || type == EntityType.HOGLIN) return "piglin";
        if (type == EntityType.SILVERFISH) return "silverfish";
        if (type == EntityType.BREEZE) return "breeze";
        if (type == EntityType.SHULKER) return "shulker";
        return "other";
    }
}
