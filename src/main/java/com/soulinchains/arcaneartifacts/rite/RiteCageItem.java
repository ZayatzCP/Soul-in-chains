package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Ritual Cage. Use it on a creature worn down to half health to shut it in (riders and mount go in together). A full cage
 * is set down on the ground to be offered in a rite; break it to take it back, sneak-use it with an empty hand to let the
 * creature out.
 */
public class RiteCageItem extends Item {
    public static final String CREATURE = "creature";
    public static final String KIND = "kind";
    public static final String VALUE = "value";
    public static final String OTHERWORLDLY = "otherworldly";

    public RiteCageItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static boolean full(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(CREATURE);
    }

    public static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** Shuts the creature (and whatever rides with it) in; returns the filled cage or null. */
    public static ItemStack capture(Player player, LivingEntity target) {
        if (!CageValue.cageable(target, player.getAbilities().instabuild)) {
            return null;
        }
        Entity root = target.getRootVehicle();
        CompoundTag saved = new CompoundTag();
        if (!root.save(saved)) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put(CREATURE, saved);
        tag.putString(KIND, root.getType().getDescriptionId());
        tag.putInt(VALUE, CageValue.of(root));
        tag.putBoolean(OTHERWORLDLY, root.getSelfAndPassengers().anyMatch(e -> CageValue.otherworldly(e.getType())));
        ItemStack cage = new ItemStack(com.soulinchains.arcaneartifacts.registry.ModItems.RITE_CAGE.get());
        cage.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        root.getSelfAndPassengers().toList().forEach(Entity::discard);
        return cage;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (full(stack)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack filled = capture(player, target);
        if (filled == null) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.cage_refused"), true);
            return InteractionResult.FAIL;
        }
        player.setItemInHand(hand, filled);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!full(stack)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced() || !level.getBlockState(pos.above()).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState state = ModBlocks.RITE_CAGE.get().defaultBlockState();
        level.setBlock(pos, state, 3);
        level.setBlock(pos.above(), state.setValue(RiteCageBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
        if (level.getBlockEntity(pos) instanceof RiteCageBlockEntity cage) {
            cage.fill(data(stack));
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
        return InteractionResult.CONSUME;
    }

    /** Lets the creature out at {@code pos}. */
    public static void release(ServerLevel level, CompoundTag data, BlockPos pos) {
        CompoundTag saved = data.getCompound(CREATURE);
        if (saved.isEmpty()) {
            return;
        }
        Entity entity = net.minecraft.world.entity.EntityType.loadEntityRecursive(saved, level, e -> {
            e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, e.getYRot(), e.getXRot());
            return e;
        });
        if (entity != null) {
            level.addFreshEntityWithPassengers(entity);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        if (full(stack)) {
            CompoundTag data = data(stack);
            return Component.translatable("item.arcane_artifacts.rite_cage.full", Component.translatable(data.getString(KIND)));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (full(stack)) {
            CompoundTag data = data(stack);
            tooltip.add(Component.translatable("arcane_artifacts.rite.cage_value", data.getInt(VALUE)).withStyle(ChatFormatting.GOLD));
            if (data.getBoolean(OTHERWORLDLY)) {
                tooltip.add(Component.translatable("arcane_artifacts.rite.cage_otherworldly").withStyle(ChatFormatting.DARK_RED));
            }
        } else {
            tooltip.add(Component.translatable("item.arcane_artifacts.rite_cage.lore").withStyle(ChatFormatting.GRAY));
        }
    }
}
