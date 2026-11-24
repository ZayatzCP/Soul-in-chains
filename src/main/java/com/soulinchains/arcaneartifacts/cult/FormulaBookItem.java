package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Book of Formulas (Confessor): right click reads the state of the nearest circle and where your partner is; sneak +
 * right click on the seal turns it to the next rite.
 */
public class FormulaBookItem extends Item {
    private static final String[] DIRECTIONS = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};

    public FormulaBookItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof RitualSealBlockEntity seal)) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            RiteScheme scheme = seal.cycleScheme(player.isCreative() ? 3 : Math.max(1, Cult.get(player).rank()));
            player.displayClientMessage(Component.translatable("arcane_artifacts.cult.scheme_set", scheme.displayName()), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            for (Component line : read(level, player)) {
                player.displayClientMessage(line, false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** What the book tells: role and partner, the nearest circle, the rite in progress. */
    public static List<Component> read(Level level, Player player) {
        List<Component> lines = new java.util.ArrayList<>();
        CultData data = Cult.get(player);
        lines.add(Component.translatable("arcane_artifacts.cult.book.role", Component.translatable("arcane_artifacts.cult.role." + data.role().id),
                data.steady()).withStyle(ChatFormatting.DARK_RED));
        Player partner = data.partner().map(level::getPlayerByUUID).orElse(null);
        if (partner != null) {
            double dx = partner.getX() - player.getX();
            double dz = partner.getZ() - player.getZ();
            int sector = Math.floorMod(Math.round((float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG) / 45F), 8);
            lines.add(Component.translatable("arcane_artifacts.cult.book.partner", partner.getDisplayName(), Math.round(partner.distanceTo(player)),
                    Component.translatable("arcane_artifacts.cult.direction." + DIRECTIONS[sector])));
        } else if (data.paired()) {
            lines.add(Component.translatable("arcane_artifacts.cult.book.partner_away"));
        }
        RitualSealBlockEntity seal = RitualSealBlockEntity.near(level, player.blockPosition());
        if (seal != null) {
            BlockPos pos = seal.getBlockPos();
            long lit = seal.anchorsAround().stream().filter(RitualAnchorBlockEntity::active).count();
            lines.add(Component.translatable("arcane_artifacts.cult.book.seal", seal.scheme().displayName(), lit, seal.anchorsAround().size(),
                    Component.translatable(seal.censerNear() ? "arcane_artifacts.cult.book.censer_yes" : "arcane_artifacts.cult.book.censer_no"))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(seal.reagentsLine().copy().withStyle(ChatFormatting.GRAY));
            if (seal.phase() == RitualSealBlockEntity.Phase.CHANNEL) {
                lines.add(Component.translatable("arcane_artifacts.cult.quality_now", Math.round(seal.quality())).withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.2").withStyle(ChatFormatting.GRAY));
    }
}
