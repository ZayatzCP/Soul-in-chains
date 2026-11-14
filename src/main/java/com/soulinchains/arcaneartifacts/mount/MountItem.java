package com.soulinchains.arcaneartifacts.mount;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/** The folded form of a class mount: use it and the mount appears right under you, saddled. */
public class MountItem extends Item {
    private final Supplier<? extends EntityType<? extends ClassMount>> type;
    private final int descriptionLines;

    public MountItem(Item.Properties properties, Supplier<? extends EntityType<? extends ClassMount>> type, int descriptionLines) {
        super(properties.stacksTo(1));
        this.type = type;
        this.descriptionLines = descriptionLines;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isPassenger()) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server) {
            ClassMount mount = deploy(server, player, stack);
            if (mount == null) {
                return InteractionResultHolder.fail(stack);
            }
            player.startRiding(mount);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Puts the mount into the world where the player stands. */
    public ClassMount deploy(ServerLevel level, Player player, ItemStack stack) {
        ClassMount mount = type.get().create(level);
        if (mount == null) {
            return null;
        }
        mount.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0F);
        mount.setYHeadRot(player.getYRot());
        mount.yBodyRot = player.getYRot();
        mount.fromItem(stack);
        level.addFreshEntity(mount);
        return mount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String lore = getDescriptionId() + ".lore";
        if (net.minecraft.locale.Language.getInstance().has(lore)) {
            tooltip.add(Component.translatable(lore).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
        for (int i = 1; i <= descriptionLines; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
        int fuel = IronHorse.storedFuel(stack);
        if (fuel > 0) {
            tooltip.add(Component.translatable("item.arcane_artifacts.iron_horse.fuel", fuel / 20).withStyle(ChatFormatting.GOLD));
        }
    }
}
