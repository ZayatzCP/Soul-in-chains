package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Catto's items. Most of the work happens in {@link Catto}; these only collect what the player points at. */
public final class CattoItems {
    private CattoItems() {
    }

    static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    static void edit(ItemStack stack, Consumer<CompoundTag> editor) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, editor);
    }

    static void tell(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    static Component pos(BlockPos pos) {
        return Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    // ------------------------------------------------------------------ the bell

    /**
     * Golden bell "Catto's Call". Ring it to call Catto back; Shift+use on Catto hides Catto inside, use on a block lets
     * Catto out again. A hidden Catto keeps everything: skills, bag, queue.
     */
    public static final class BellItem extends Item {
        public BellItem() {
            super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        }

        public static boolean holdsCatto(ItemStack stack) {
            return data(stack).contains("Catto");
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
                if (holdsCatto(stack)) {
                    tell(player, Component.translatable("arcane_artifacts.catto.bell.release_hint"));
                } else {
                    Catto.callFor(serverPlayer);
                }
            }
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.6F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            ItemStack stack = context.getItemInHand();
            Player player = context.getPlayer();
            if (player == null || !holdsCatto(stack)) {
                return InteractionResult.PASS;
            }
            if (context.getLevel() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
                BlockPos at = context.getClickedPos().relative(context.getClickedFace());
                if (Catto.release(serverPlayer, level, stack, at)) {
                    edit(stack, tag -> tag.remove("Catto"));
                }
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return holdsCatto(stack);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            if (holdsCatto(stack)) {
                tooltip.add(Component.translatable("arcane_artifacts.catto.bell.inside").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            tooltip.add(Component.translatable("arcane_artifacts.catto.bell.hint").withStyle(ChatFormatting.GRAY));
        }
    }

    // ------------------------------------------------------------------ the wand

    /** Errand Wand: points at blocks, storages and areas. Shift+use in the air switches the mode. */
    public static final class WandItem extends Item {
        public enum Mode {
            BIND, MINE, COLLECT, BUILD, EXPLORE, FETCH, CRAFT;

            public Component title() {
                return Component.translatable("arcane_artifacts.catto.wand.mode." + name().toLowerCase(Locale.ROOT));
            }

            public Component hint() {
                return Component.translatable("arcane_artifacts.catto.wand.hint." + name().toLowerCase(Locale.ROOT));
            }
        }

        public WandItem() {
            super(new Item.Properties().stacksTo(1));
        }

        public static Mode mode(ItemStack stack) {
            int i = data(stack).getInt("Mode");
            Mode[] modes = Mode.values();
            return modes[Math.floorMod(i, modes.length)];
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide()) {
                if (player.isShiftKeyDown()) {
                    Mode next = Mode.values()[(mode(stack).ordinal() + 1) % Mode.values().length];
                    edit(stack, tag -> {
                        tag.putInt("Mode", next.ordinal());
                        tag.remove("Corner");
                    });
                    tell(player, Component.translatable("arcane_artifacts.catto.wand.switched", next.title(), next.hint()));
                } else if (data(stack).contains("Corner")) {
                    edit(stack, tag -> tag.remove("Corner"));
                    tell(player, Component.translatable("arcane_artifacts.catto.wand.corner_cleared"));
                } else {
                    tell(player, mode(stack).hint());
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }
            if (!(context.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = context.getItemInHand();
            BlockPos pos = context.getClickedPos();
            Mode mode = mode(stack);
            Catto cat = Catto.find(serverPlayer);
            if (cat == null || cat.level() != level) {
                tell(player, Component.translatable("arcane_artifacts.catto.not_here"));
                return InteractionResult.FAIL;
            }
            BlockState state = level.getBlockState(pos);
            switch (mode) {
                case BIND -> {
                    if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null) {
                        tell(player, Component.translatable("arcane_artifacts.catto.wand.not_storage"));
                    } else {
                        cat.toggleStorage(serverPlayer, pos, player.isShiftKeyDown());
                    }
                }
                case MINE -> {
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
                        tell(player, Component.translatable("arcane_artifacts.catto.wand.cannot_mine"));
                    } else {
                        cat.offer(serverPlayer, CattoTask.mine(state.getBlock(), pos, player.isShiftKeyDown() ? 16 : CattoTask.DEFAULT_COUNT));
                    }
                }
                case COLLECT -> {
                    ItemStack filter = player.getOffhandItem();
                    if (player.isShiftKeyDown()) {
                        cat.offer(serverPlayer, CattoTask.collect(pos, null, filter));
                    } else {
                        corner(stack, serverPlayer, pos, corner -> cat.offer(serverPlayer, CattoTask.collect(corner, pos, filter)));
                    }
                }
                case BUILD -> {
                    ItemStack offhand = player.getOffhandItem();
                    if (offhand.getItem() instanceof BlueprintItem && BlueprintItem.blocks(offhand) > 0) {
                        cat.offer(serverPlayer, CattoTask.blueprint(pos.relative(context.getClickedFace()),
                                data(offhand).getCompound("Structure"), BlueprintItem.blocks(offhand)));
                    } else if (!(offhand.getItem() instanceof BlockItem)) {
                        tell(player, Component.translatable("arcane_artifacts.catto.wand.need_material"));
                    } else {
                        corner(stack, serverPlayer, pos, corner -> cat.offer(serverPlayer, CattoTask.fill(corner, pos, offhand)));
                    }
                }
                case EXPLORE -> cat.offer(serverPlayer, CattoTask.explore(pos));
                case FETCH, CRAFT -> tell(player, Component.translatable("arcane_artifacts.catto.wand.use_on_cat"));
            }
            return InteractionResult.SUCCESS;
        }

        /** First click remembers a corner, the second one gives the area to {@code done}. */
        private static void corner(ItemStack stack, ServerPlayer player, BlockPos pos, Consumer<BlockPos> done) {
            CompoundTag tag = data(stack);
            if (!tag.contains("Corner")) {
                edit(stack, t -> t.putLong("Corner", pos.asLong()));
                tell(player, Component.translatable("arcane_artifacts.catto.wand.corner", pos(pos)));
                return;
            }
            BlockPos corner = BlockPos.of(tag.getLong("Corner"));
            edit(stack, t -> t.remove("Corner"));
            done.accept(corner);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return data(stack).contains("Corner");
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            Mode mode = mode(stack);
            tooltip.add(Component.translatable("arcane_artifacts.catto.wand.current", mode.title()).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(mode.hint().copy().withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("arcane_artifacts.catto.wand.switch_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // ------------------------------------------------------------------ the scroll

    /** Errand Scroll: Shift+use on Catto writes the queue onto it (or reads it back when something is written). */
    public static final class ScrollItem extends Item {
        public ScrollItem() {
            super(new Item.Properties().stacksTo(1));
        }

        public static ListTag program(ItemStack stack) {
            return data(stack).getList("Program", Tag.TAG_COMPOUND);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && !program(stack).isEmpty()) {
                if (!level.isClientSide()) {
                    edit(stack, tag -> tag.remove("Program"));
                    tell(player, Component.translatable("arcane_artifacts.catto.scroll.cleared"));
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            return InteractionResultHolder.pass(stack);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return !program(stack).isEmpty();
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            ListTag program = program(stack);
            if (program.isEmpty()) {
                tooltip.add(Component.translatable("arcane_artifacts.catto.scroll.empty").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("arcane_artifacts.catto.scroll.steps", program.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (context.registries() != null) {
                    for (Tag t : program) {
                        CattoTask task = CattoTask.load((CompoundTag) t, context.registries());
                        if (task != null) {
                            tooltip.add(Component.literal("• ").append(task.type.title()).append(": ").append(task.target()).withStyle(ChatFormatting.GRAY));
                        }
                    }
                }
            }
            tooltip.add(Component.translatable("arcane_artifacts.catto.scroll.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // ------------------------------------------------------------------ the blueprint

    /** Blueprint Parchment: two clicks copy up to 16x16x16 blocks; Catto (Builder II) builds the copy. */
    public static final class BlueprintItem extends Item {
        public static final int MAX_SIZE = 16;
        public static final int MAX_BLOCKS = 1024;

        public BlueprintItem() {
            super(new Item.Properties().stacksTo(1));
        }

        public static int blocks(ItemStack stack) {
            return data(stack).getCompound("Structure").getInt("Count");
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    edit(stack, tag -> {
                        tag.remove("Structure");
                        tag.remove("Corner");
                    });
                    tell(player, Component.translatable("arcane_artifacts.catto.blueprint.cleared"));
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            return InteractionResultHolder.pass(stack);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            if (player == null || context.getLevel().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = context.getItemInHand();
            BlockPos pos = context.getClickedPos();
            CompoundTag tag = data(stack);
            if (!tag.contains("Corner")) {
                edit(stack, t -> t.putLong("Corner", pos.asLong()));
                tell(player, Component.translatable("arcane_artifacts.catto.wand.corner", pos(pos)));
                return InteractionResult.SUCCESS;
            }
            BlockPos corner = BlockPos.of(tag.getLong("Corner"));
            CompoundTag structure = capture(context.getLevel(), corner, pos);
            if (structure == null) {
                edit(stack, t -> t.remove("Corner"));
                tell(player, Component.translatable("arcane_artifacts.catto.blueprint.too_big", MAX_SIZE, MAX_BLOCKS));
                return InteractionResult.FAIL;
            }
            edit(stack, t -> {
                t.remove("Corner");
                t.put("Structure", structure);
            });
            tell(player, Component.translatable("arcane_artifacts.catto.blueprint.copied", structure.getInt("Count")));
            return InteractionResult.SUCCESS;
        }

        /** Size, palette of block states and a palette index per position (-1 = leave as it is). */
        @org.jetbrains.annotations.Nullable
        public static CompoundTag capture(Level level, BlockPos a, BlockPos b) {
            BlockPos min = BlockPos.min(a, b);
            BlockPos max = BlockPos.max(a, b);
            int sx = max.getX() - min.getX() + 1;
            int sy = max.getY() - min.getY() + 1;
            int sz = max.getZ() - min.getZ() + 1;
            if (sx > MAX_SIZE || sy > MAX_SIZE || sz > MAX_SIZE) {
                return null;
            }
            List<BlockState> palette = new java.util.ArrayList<>();
            int[] cells = new int[sx * sy * sz];
            int count = 0;
            int i = 0;
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    for (int x = 0; x < sx; x++) {
                        BlockState state = level.getBlockState(min.offset(x, y, z));
                        if (state.isAir() || !(state.getBlock().asItem() instanceof BlockItem)) {
                            cells[i++] = -1;
                            continue;
                        }
                        int index = palette.indexOf(state);
                        if (index < 0) {
                            palette.add(state);
                            index = palette.size() - 1;
                        }
                        cells[i++] = index;
                        count++;
                    }
                }
            }
            if (count == 0 || count > MAX_BLOCKS) {
                return null;
            }
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("Size", new int[]{sx, sy, sz});
            ListTag list = new ListTag();
            palette.forEach(state -> list.add(NbtUtils.writeBlockState(state)));
            tag.put("Palette", list);
            tag.putIntArray("Cells", cells);
            tag.putInt("Count", count);
            return tag;
        }

        /** The positions and states of a structure placed with its lowest corner at {@code origin}, bottom layer first. */
        public static List<java.util.Map.Entry<BlockPos, BlockState>> layout(Level level, CompoundTag structure, BlockPos origin) {
            List<java.util.Map.Entry<BlockPos, BlockState>> out = new java.util.ArrayList<>();
            int[] size = structure.getIntArray("Size");
            int[] cells = structure.getIntArray("Cells");
            if (size.length != 3) {
                return out;
            }
            ListTag list = structure.getList("Palette", Tag.TAG_COMPOUND);
            List<BlockState> palette = new java.util.ArrayList<>();
            var blocks = level.holderLookup(Registries.BLOCK);
            for (Tag t : list) {
                palette.add(NbtUtils.readBlockState(blocks, (CompoundTag) t));
            }
            int i = 0;
            for (int y = 0; y < size[1]; y++) {
                for (int z = 0; z < size[2]; z++) {
                    for (int x = 0; x < size[0]; x++) {
                        int index = i < cells.length ? cells[i] : -1;
                        i++;
                        if (index >= 0 && index < palette.size()) {
                            out.add(java.util.Map.entry(origin.offset(x, y, z), palette.get(index)));
                        }
                    }
                }
            }
            return out;
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return blocks(stack) > 0;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            int blocks = blocks(stack);
            if (blocks > 0) {
                int[] size = data(stack).getCompound("Structure").getIntArray("Size");
                if (size.length == 3) {
                    tooltip.add(Component.translatable("arcane_artifacts.catto.blueprint.holds", size[0], size[1], size[2], blocks)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            tooltip.add(Component.translatable("arcane_artifacts.catto.blueprint.hint").withStyle(ChatFormatting.GRAY));
        }
    }

    // ------------------------------------------------------------------ bags and modules

    /** A bag for Catto's bag slot: how many of the 27 bag slots Catto can use. */
    public static final class PackItem extends Item {
        public final int capacity;

        public PackItem(int capacity, Rarity rarity) {
            super(new Item.Properties().stacksTo(1).rarity(rarity));
            this.capacity = capacity;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("arcane_artifacts.catto.pack.slots", capacity).withStyle(ChatFormatting.GRAY));
        }
    }

}
