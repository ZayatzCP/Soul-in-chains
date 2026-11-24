package com.soulinchains.arcaneartifacts.druid;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarvest;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.soulinchains.arcaneartifacts.glyph.EffectBeeSwarm;
import com.soulinchains.arcaneartifacts.glyph.EffectEntangle;
import com.soulinchains.arcaneartifacts.glyph.EffectGrove;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;

import java.util.List;
import java.util.function.Supplier;

/**
 * A druid's staff. Each of the five holds one spell of Root Magic (a "circle"); the Staff of the Five Circles, made of
 * all five at the Enchanting Apparatus, holds them all - the circle key (B by default) switches between them - and
 * casts 15% cheaper and 25% faster. Spells are cast through Ars Nouveau with the caster's own mana.
 */
public class DruidStaffItem extends Item {
    public static final float GRAND_DISCOUNT = 0.15F;
    public static final float GRAND_COOLDOWN = 0.75F;

    public enum Circle {
        ROOTS("roots", 30, () -> new Spell(MethodProjectile.INSTANCE, EffectEntangle.INSTANCE, AugmentAOE.INSTANCE)),
        GROVE("grove", 40, () -> new Spell(MethodProjectile.INSTANCE, EffectGrove.INSTANCE, AugmentAOE.INSTANCE, AugmentAOE.INSTANCE, EffectHarvest.INSTANCE)),
        HIVE("hive", 60, () -> new Spell(MethodProjectile.INSTANCE, EffectBeeSwarm.INSTANCE)),
        SAP("sap", 100, () -> new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE, AugmentAOE.INSTANCE)),
        THORNS("thorns", 200, null);

        public final String id;
        public final int cooldown;
        private final Supplier<Spell> spell;

        Circle(String id, int cooldown, Supplier<Spell> spell) {
            this.id = id;
            this.cooldown = cooldown;
            this.spell = spell;
        }

        public Component displayName() {
            return Component.translatable("arcane_artifacts.druid_circle." + id);
        }

        public Circle next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static final int THORNS_MANA = 40;
    public static final int THORNS_TICKS = 300;
    public static final int THORNS_WIDTH = 5;

    /** Null for the Staff of the Five Circles. */
    private final Circle fixed;

    public DruidStaffItem(Circle fixed) {
        super(new Properties().stacksTo(1).rarity(fixed == null ? Rarity.EPIC : Rarity.UNCOMMON)
                .component(ModComponents.DRUID_CIRCLE, 0));
        this.fixed = fixed;
    }

    public boolean grand() {
        return fixed == null;
    }

    public Circle circle(ItemStack stack) {
        if (fixed != null) {
            return fixed;
        }
        Circle[] all = Circle.values();
        return all[Math.floorMod(stack.getOrDefault(ModComponents.DRUID_CIRCLE, 0), all.length)];
    }

    /** The circle key: the Staff of the Five Circles turns to its next circle. */
    public static boolean switchCircle(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof DruidStaffItem staff && staff.grand()) {
                Circle next = staff.circle(stack).next();
                stack.set(ModComponents.DRUID_CIRCLE, next.ordinal());
                player.displayClientMessage(Component.translatable("arcane_artifacts.druid_staff.circle", next.displayName()).withStyle(ChatFormatting.GREEN), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 0.8F, 0.8F + next.ordinal() * 0.1F);
                return true;
            }
        }
        return false;
    }

    public static boolean holdsGrandStaff(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof DruidStaffItem staff && staff.grand()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        Circle circle = circle(stack);
        boolean cast = circle == Circle.THORNS ? thorns((ServerLevel) level, player) : cast(level, player, stack, circle);
        if (!cast) {
            return InteractionResultHolder.fail(stack);
        }
        if (circle == Circle.SAP) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        }
        player.getCooldowns().addCooldown(this, Math.round(circle.cooldown * (grand() ? GRAND_COOLDOWN : 1F)));
        player.swing(hand, true);
        return InteractionResultHolder.consume(stack);
    }

    private static boolean cast(Level level, Player player, ItemStack stack, Circle circle) {
        SpellContext context = SpellContext.fromEntity(circle.spell.get(), player, stack);
        return new SpellResolver(context).onCast(stack, level);
    }

    /** The Thorns circle: a wall of ripe berry bushes across your way, gone again after 15 seconds. */
    public static boolean thorns(ServerLevel level, Player player) {
        var mana = CapabilityRegistry.getMana(player);
        int cost = Math.round(THORNS_MANA * (com.soulinchains.arcaneartifacts.util.CurioHelper.findEquipped(player,
                com.soulinchains.arcaneartifacts.registry.ModItems.DRUID_HAT.get()).isPresent() ? 1F - DruidGearItem.HAT_DISCOUNT : 1F));
        if (!player.isCreative() && (mana == null || mana.getCurrentMana() < cost)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.druid_staff.no_mana"), true);
            return false;
        }
        Direction facing = player.getDirection();
        Direction side = facing.getClockWise();
        BlockPos centre = player.blockPosition().relative(facing, 3);
        int placed = 0;
        for (int w = -THORNS_WIDTH / 2; w <= THORNS_WIDTH / 2; w++) {
            BlockPos column = centre.relative(side, w);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos pos = column.above(dy);
                if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
                    level.setBlock(pos, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3), 3);
                    ThornWalls.add(level, pos, level.getGameTime() + THORNS_TICKS);
                    level.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
                    placed++;
                    break;
                }
            }
        }
        if (placed == 0) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.druid_staff.no_ground"), true);
            return false;
        }
        if (!player.isCreative() && mana != null) {
            mana.removeMana(cost);
        }
        level.playSound(null, centre, SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.PLAYERS, 1.2F, 0.7F);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (grand()) {
            tooltip.add(Component.translatable("item.arcane_artifacts.druid_staff_grand.desc.1").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("arcane_artifacts.druid_staff.circle", circle(stack).displayName()).withStyle(ChatFormatting.GREEN));
            for (Circle circle : Circle.values()) {
                tooltip.add(Component.literal(" • ").append(circle.displayName()).append(": ")
                        .append(Component.translatable("arcane_artifacts.druid_circle." + circle.id + ".desc")).withStyle(ChatFormatting.DARK_GREEN));
            }
            tooltip.add(Component.translatable("item.arcane_artifacts.druid_staff_grand.desc.2").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("arcane_artifacts.druid_circle." + fixed.id + ".desc").withStyle(ChatFormatting.GRAY));
        }
    }

}
