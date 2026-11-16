package com.soulinchains.arcaneartifacts.druid;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Root Magic, the Druid's school, and the Druid's gear: a herbalist-alchemist who gathers what grows and
 * brews it. Also "living ground" (grass, moss, leaves, dirt, mycelium), which the Moss Elk draws on.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RootMagic {
    private RootMagic() {
    }

    /** Grass, moss, leaves, dirt, mycelium, or standing in grass and flowers. */
    public static boolean onLivingGround(Entity entity) {
        BlockPos below = entity.getBlockPosBelowThatAffectsMyMovement();
        BlockState state = entity.level().getBlockState(below);
        return state.is(BlockTags.DIRT) || state.is(BlockTags.LEAVES) || state.is(Blocks.MOSS_CARPET)
                || entity.level().getBlockState(entity.blockPosition()).is(BlockTags.REPLACEABLE_BY_TREES);
    }

    public static boolean wears(LivingEntity entity, net.minecraft.world.item.Item item) {
        return CurioHelper.findEquipped(entity, item).isPresent();
    }

    /** All four pieces of the Druid's gear. */
    public static boolean fullSet(LivingEntity entity) {
        return wears(entity, ModItems.DRUID_MANTLE.get()) && wears(entity, ModItems.DRUID_HAT.get())
                && wears(entity, ModItems.DRUID_GLOVES.get()) && wears(entity, ModItems.DRUID_BOOTS.get());
    }

    /** What a herbalist gathers: mushrooms (magic ones too), cave plants, flowers, berries, crops, nether wart. */
    public static boolean isHerb(BlockState state) {
        return state.getBlock() instanceof com.soulinchains.arcaneartifacts.block.ArcaneMushroomBlock
                || state.getBlock() instanceof com.soulinchains.arcaneartifacts.cave.CavePlantBlock
                || state.getBlock() instanceof MushroomBlock || state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.NETHER_WART) || state.is(Blocks.GLOW_LICHEN)
                || state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT) || state.is(Blocks.MOSS_BLOCK);
    }

    /** The hat: magic mushrooms and herbs within the radius sparkle for the wearer only. Returns how many. */
    public static int sniff(Player player, int radius) {
        if (!(player.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer viewer)) {
            return 0;
        }
        int found = 0;
        BlockPos centre = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius / 2, -radius), centre.offset(radius, radius / 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof com.soulinchains.arcaneartifacts.block.ArcaneMushroomBlock
                    || state.getBlock() instanceof com.soulinchains.arcaneartifacts.cave.CavePlantBlock) {
                level.sendParticles(viewer, ParticleTypes.HAPPY_VILLAGER, true, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
                if (++found >= 40) {
                    break;
                }
            }
        }
        return found;
    }

    /** The gloves: half the time a herb gives its harvest twice over. */
    @SubscribeEvent
    public static void onHarvest(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof LivingEntity breaker) || !isHerb(event.getState())
                || !wears(breaker, ModItems.DRUID_GLOVES.get())) {
            return;
        }
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            if (breaker.getRandom().nextFloat() < DruidGearItem.EXTRA_HARVEST) {
                extra.add(new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
        }
        event.getDrops().addAll(extra);
    }

    /** The mantle (potions last longer) and the full set (a potion may not be used up). */
    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Finish event) {
        LivingEntity drinker = event.getEntity();
        ItemStack used = event.getItem();
        if (drinker.level().isClientSide || !(used.getItem() instanceof PotionItem)) {
            return;
        }
        if (wears(drinker, ModItems.DRUID_MANTLE.get())) {
            lengthen(drinker, used.get(DataComponents.POTION_CONTENTS));
        }
        if (fullSet(drinker) && drinker.getRandom().nextFloat() < DruidGearItem.FREE_POTION) {
            event.setResultStack(used.copyWithCount(1));
        }
    }

    /** Re-applies a potion's lasting effects 30% longer. Returns how many were lengthened. */
    public static int lengthen(LivingEntity drinker, PotionContents contents) {
        if (contents == null) {
            return 0;
        }
        int lengthened = 0;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (!effect.getEffect().value().isInstantenous()) {
                drinker.addEffect(new MobEffectInstance(effect.getEffect(), Math.round(effect.getDuration() * DruidGearItem.POTION_LONGER),
                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
                lengthened++;
            }
        }
        return lengthened;
    }

    /** Druid's Boots: farmland is not trampled. */
    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof LivingEntity living && wears(living, ModItems.DRUID_BOOTS.get())) {
            event.setCanceled(true);
        }
    }
}
