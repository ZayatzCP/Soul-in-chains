package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Accessory sets and their full-set bonuses. A piece counts when it is worn (Curios) or held in either hand.
 * The Wreath of Manipulation belongs to no set.
 * <ul>
 *     <li>Focusing (boots, gloves, shield): spells cost 25% less mana.</li>
 *     <li>Reflection (mantle, ring, mirror shield): projectiles never hurt you, they fly back at the shooter.</li>
 *     <li>Poor Magic (poncho, hat, boots, gloves, bandolier): guns reload instantly, 15% of shots use no round.</li>
 *     <li>Amanitoid (mushroom shield and club): immune to spores, the club hits 25% harder.</li>
 *     <li>Druid (mantle, hat, gloves, boots): a drunk potion is not used up 30% of the time (see RootMagic).</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class ArcaneSets {
    public enum Set {
        FOCUSING("focusing", List.of(ModItems.FOCUSING_BOOTS, ModItems.FOCUSING_GLOVES, ModItems.FOCUSING_SHIELD)),
        REFLECTION("reflection", List.of(ModItems.MANTLE_OF_REFLECTION, ModItems.RING_OF_REVERSAL, ModItems.MIRROR_SHIELD)),
        POOR_MAGIC("poor_magic", List.of(ModItems.PONCHO, ModItems.SOMBRERO, ModItems.PSEUDOMAGE_BOOTS, ModItems.PSEUDOMAGE_GLOVES, ModItems.BANDOLIER)),
        AMANITOID("amanitoid", List.of(ModItems.MUSHROOM_SHIELD, ModItems.MUSHROOM_CLUB)),
        DRUID("druid", List.of(ModItems.DRUID_MANTLE, ModItems.DRUID_HAT, ModItems.DRUID_GLOVES, ModItems.DRUID_BOOTS)),
        SHACKLED("shackled", List.of(ModItems.SOUL_LANTERN, ModItems.SHACKLE_GLOVES, ModItems.SHROUD, ModItems.BONE_CROWN,
                ModItems.PHYLACTERY_AMULET, ModItems.SHACKLED_BOOTS));

        public final String id;
        public final List<? extends Supplier<? extends Item>> pieces;

        Set(String id, List<? extends Supplier<? extends Item>> pieces) {
            this.id = id;
            this.pieces = pieces;
        }

        public int worn(LivingEntity entity) {
            int count = 0;
            for (Supplier<? extends Item> piece : pieces) {
                if (has(entity, piece.get()) || piece.get() == ModItems.BONE_CROWN.get() && has(entity, ModItems.KINGS_CROWN.get())) {
                    count++;
                }
            }
            return count;
        }

        public boolean complete(LivingEntity entity) {
            return worn(entity) == pieces.size();
        }

        public boolean contains(Item item) {
            return pieces.stream().anyMatch(p -> p.get() == item);
        }
    }

    public static final float FOCUS_DISCOUNT = 0.25F;
    public static final float FREE_ROUND = 0.15F;
    public static final float CLUB_BONUS = 1.25F;

    private ArcaneSets() {
    }

    /** Worn in a Curios slot or held in either hand. */
    public static boolean has(LivingEntity entity, Item item) {
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item) || CurioHelper.findEquipped(entity, item).isPresent();
    }

    public static Set setOf(Item item) {
        for (Set set : Set.values()) {
            if (set.contains(item)) {
                return set;
            }
        }
        return null;
    }

    /** Tooltip lines for a set piece: "Set: Focusing (2/3)" and the bonus. */
    public static void appendTooltip(Item item, List<Component> tooltip, LivingEntity viewer) {
        Set set = setOf(item);
        if (set == null) {
            return;
        }
        int worn = viewer == null ? 0 : set.worn(viewer);
        boolean complete = worn == set.pieces.size();
        tooltip.add(Component.translatable("arcane_artifacts.set.header", Component.translatable("arcane_artifacts.set." + set.id), worn, set.pieces.size())
                .withStyle(complete ? ChatFormatting.GREEN : ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("arcane_artifacts.set." + set.id + ".bonus").withStyle(complete ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
    }

    // ------------------------------------------------------------------ Focusing: cheaper spells

    public static int discounted(LivingEntity caster, int cost) {
        return Set.FOCUSING.complete(caster) ? Math.round(cost * (1F - FOCUS_DISCOUNT)) : cost;
    }

    @SubscribeEvent
    public static void onCostPre(SpellCostCalcEvent.Pre event) {
        event.currentCost = discounted(event.context.getUnwrappedCaster(), event.currentCost);
    }

    @SubscribeEvent
    public static void onCostPost(SpellCostCalcEvent.Post event) {
        event.currentCost = discounted(event.context.getUnwrappedCaster(), event.currentCost);
    }

    // ------------------------------------------------------------------ Reflection and Amanitoid: damage

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getDirectEntity() instanceof Projectile projectile && Set.REFLECTION.complete(target)) {
            event.setCanceled(true);
            bounce(projectile, target);
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living && event.getSource().getDirectEntity() == attacker
                && living.getMainHandItem().is(ModItems.MUSHROOM_CLUB.get()) && Set.AMANITOID.complete(living)) {
            event.setAmount(event.getAmount() * CLUB_BONUS);
        }
    }

    /** A projectile turned back: it flies at whoever shot it, and now belongs to the one it hit. */
    public static void bounce(Projectile projectile, LivingEntity reflector) {
        Entity shooter = projectile.getOwner();
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = Math.max(0.6, velocity.length());
        Vec3 back = shooter != null ? shooter.getEyePosition().subtract(projectile.position()).normalize().scale(speed) : velocity.scale(-1);
        projectile.setOwner(reflector);
        projectile.setDeltaMovement(back);
        projectile.hasImpulse = true;
    }

    // ------------------------------------------------------------------ Amanitoid: no spores

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player.tickCount % 10 == 0 && player.hasEffect(ModEffects.SPORE_INFECTION)
                && Set.AMANITOID.complete(player)) {
            player.removeEffect(ModEffects.SPORE_INFECTION);
        }
    }

    /** Poor Magic: a free round. */
    public static boolean freeRound(Player shooter) {
        return Set.POOR_MAGIC.complete(shooter) && shooter.getRandom().nextFloat() < FREE_ROUND;
    }

    public static boolean instantReload(Player shooter) {
        return Set.POOR_MAGIC.complete(shooter);
    }
}
