package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ritual chalk. Use on the top of a floor block to set a mark; use on the next block within 3 s to draw a line to it (a
 * stroke). Use on a mark to carry the stroke on from it - or, during a rite, to trace a fading sigil. Sneak to rub a mark out.
 * Every mark and every line costs one use; a chalk lasts 64.
 */
public class ChalkItem extends Item {
    public static final int USES = 64;
    public static final int STROKE_TICKS = 60;
    private static final Map<UUID, Stroke> STROKES = new HashMap<>();
    public final ChalkColor color;

    private record Stroke(BlockPos last, long at) {
    }

    public ChalkItem(ChalkColor color) {
        super(new Item.Properties().durability(USES));
        this.color = color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        BlockPos mark;
        if (state.getBlock() instanceof ChalkMarkBlock || state.getBlock() instanceof OfferingPedestalBlock) {
            mark = clicked;
        } else if (context.getClickedFace() == Direction.UP) {
            mark = clicked.above();
        } else {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        draw(level, player, context.getItemInHand(), mark, color);
        return InteractionResult.CONSUME;
    }

    /** One touch of the chalk at {@code mark}. Returns what happened, for tests: 0 nothing, 1 mark, 2 line, 3 erased, 4 traced. */
    public static int draw(Level level, Player player, @Nullable ItemStack chalk, BlockPos mark, ChalkColor color) {
        BlockState state = level.getBlockState(mark);
        boolean pedestal = state.getBlock() instanceof OfferingPedestalBlock;
        boolean existing = ChalkMarkBlock.holdsChalk(state);
        if (existing && player.isShiftKeyDown()) {
            if (pedestal) {
                level.setBlock(mark, OfferingPedestalBlock.clean(state), 3);
            } else {
                level.removeBlock(mark, false);
            }
            level.playSound(null, mark, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.5F, 1.4F);
            STROKES.remove(player.getUUID());
            return 3;
        }
        if (existing && RiteHeartBlockEntity.traceAt(level, mark, player, color)) {
            spend(player, chalk);
            return 4;
        }
        int result = 0;
        if (!existing && pedestal) {
            if (player.isShiftKeyDown()) {
                return 0;
            }
            level.setBlock(mark, state.setValue(OfferingPedestalBlock.CHALKED, true).setValue(ChalkMarkBlock.COLOR, color), 3);
            spend(player, chalk);
            result = 1;
        } else if (!existing) {
            if (!level.getBlockState(mark).canBeReplaced() || !ModBlocks.CHALK_MARK.get().defaultBlockState().canSurvive(level, mark)) {
                return 0;
            }
            level.setBlock(mark, ModBlocks.CHALK_MARK.get().defaultBlockState().setValue(ChalkMarkBlock.COLOR, color), 3);
            spend(player, chalk);
            result = 1;
        } else if (state.getValue(ChalkMarkBlock.COLOR) != color) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.chalk_other_color"), true);
            return 0;
        }
        Stroke stroke = STROKES.get(player.getUUID());
        long now = level.getGameTime();
        if (stroke != null && now - stroke.at <= STROKE_TICKS && ChalkMarkBlock.join(level, stroke.last, mark)) {
            spend(player, chalk);
            result = 2;
        }
        STROKES.put(player.getUUID(), new Stroke(mark.immutable(), now));
        level.playSound(null, mark, SoundEvents.CALCITE_HIT, SoundSource.BLOCKS, 0.6F, 1.5F);
        return result;
    }

    /** Forgets the player's current stroke (the next touch starts a new one). */
    public static void endStroke(Player player) {
        STROKES.remove(player.getUUID());
    }

    private static void spend(Player player, @Nullable ItemStack chalk) {
        if (chalk != null && !chalk.isEmpty() && !player.getAbilities().instabuild
                && !(CultGearItem.wears(player, CultGearItem.Piece.GLOVES) && player.getRandom().nextBoolean())) {
            chalk.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.arcane_artifacts.chalk.lore").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
