package com.soulinchains.arcaneartifacts.necro;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Warden's Rod: commands the servants.
 * <ul>
 *     <li>Right click a creature: every servant attacks it.</li>
 *     <li>Right click your servant: it switches between following you and guarding its spot.</li>
 *     <li>Sneak + right click your servant: dismiss it (its key soul goes back to the lantern).</li>
 *     <li>Right click the air: everyone follows you. Sneak + right click: everyone guards the spot you look at.</li>
 * </ul>
 */
public class WardenRodItem extends Item {
    public WardenRodItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (target instanceof Servant servant && player.getUUID().equals(servant.getOwnerUUID())) {
            if (player.isShiftKeyDown()) {
                servant.crumble(true);
                say(player, "arcane_artifacts.warden_rod.dismissed", servant.getTypeName());
            } else if (servant.command() == Servant.Command.FOLLOW) {
                servant.guard(servant.blockPosition());
                say(player, "arcane_artifacts.warden_rod.guard_one", servant.getTypeName());
            } else {
                servant.follow();
                say(player, "arcane_artifacts.warden_rod.follow_one", servant.getTypeName());
            }
            return InteractionResult.CONSUME;
        }
        Set<Servant> servants = Servant.of(player.getUUID());
        int sent = 0;
        for (Servant servant : servants) {
            if (servant.kind.fights() && servant.canAttack(target)) {
                servant.setTarget(target);
                sent++;
            }
        }
        say(player, "arcane_artifacts.warden_rod.attack", sent, target.getDisplayName());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        Set<Servant> servants = Servant.of(player.getUUID());
        if (player.isShiftKeyDown()) {
            BlockPos post = lookedAt(player);
            for (Servant servant : servants) {
                servant.setTarget(null);
                servant.guard(post);
            }
            say(player, "arcane_artifacts.warden_rod.guard_all", servants.size());
        } else {
            for (Servant servant : servants) {
                servant.setTarget(null);
                servant.follow();
            }
            say(player, "arcane_artifacts.warden_rod.follow_all", servants.size());
        }
        return InteractionResultHolder.consume(stack);
    }

    private static BlockPos lookedAt(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(32));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection()) : player.blockPosition();
    }

    private static void say(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
