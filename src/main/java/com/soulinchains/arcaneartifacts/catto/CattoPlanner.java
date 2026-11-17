package com.soulinchains.arcaneartifacts.catto;

import com.soulinchains.arcaneartifacts.catto.CattoBrain.Plan;
import com.soulinchains.arcaneartifacts.catto.CattoBrain.Step;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Turns an errand into the next concrete step, and carries steps out. Every errand ends by bringing its result: to the
 * owner (fetching, crafting) or to the bound storages (mining, gathering), with the owner as the fallback.
 */
public final class CattoPlanner {
    private CattoPlanner() {
    }

    static Plan plan(Catto cat, CattoBrain brain, CattoTask task) {
        Map<String, Object> memo = brain.memo(task);
        return switch (task.type) {
            case MINE -> mine(cat, brain, task);
            case COLLECT -> collect(cat, brain, task);
            case FETCH -> fetch(cat, task);
            case CRAFT -> craft(cat, brain, task, memo);
            case BUILD -> build(cat, brain, task, memo);
            case EXPLORE -> explore(cat, brain, task, memo);
        };
    }

    // ------------------------------------------------------------------ shared pieces

    private static Step step(Step.Kind kind, BlockPos pos, double reach, int workTicks) {
        Step step = new Step(kind, pos);
        step.reach = reach;
        step.workTicks = workTicks;
        step.accuracy = Math.max(1, (int) Math.floor(reach) - 1);
        return step;
    }

    private static CattoScanner scanner(CattoBrain brain, String key, Supplier<CattoScanner> factory) {
        if (brain.scanner == null || !brain.scanner.key.equals(key)) {
            brain.scanner = factory.get();
        }
        return brain.scanner;
    }

    /**
     * The next step of bringing matching bag items away, or null when there is nothing to bring.
     * {@code toOwner}: hand them to the owner (storages if the owner cannot be reached); otherwise storages first.
     */
    @Nullable
    private static Plan deliver(Catto cat, Predicate<ItemStack> what, boolean toOwner, int amount) {
        if (CattoWorld.bagCount(cat, what) == 0) {
            return null;
        }
        ServerPlayer owner = cat.ownerPlayer();
        boolean ownerHere = owner != null && owner.level() == cat.level() && owner.isAlive() && !owner.isSpectator();
        if (!toOwner || !ownerHere) {
            BlockPos storage = CattoWorld.storageFor(cat, what);
            if (storage != null) {
                Step s = step(Step.Kind.PUT, storage, 2.5, 8);
                s.filter = what;
                return Plan.step(s);
            }
        }
        if (ownerHere) {
            Step s = step(Step.Kind.GIVE, owner.blockPosition(), 2.0, 6);
            s.entity = owner;
            s.filter = what;
            s.amount = amount;
            s.blacklistOnFail = false;
            return Plan.step(s);
        }
        return Plan.blocked(Component.translatable(cat.storages.isEmpty()
                ? "arcane_artifacts.catto.blocked.nowhere_to_put" : "arcane_artifacts.catto.blocked.storages_full"));
    }

    // ------------------------------------------------------------------ mine

    private static Plan mine(Catto cat, CattoBrain brain, CattoTask task) {
        Block block = task.block == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(task.block);
        if (block == Blocks.AIR) {
            return Plan.DONE;
        }
        Level level = cat.level();
        if (task.phase == 0 && task.done >= task.count) {
            task.phase = 1;
        }
        if (task.phase == 0) {
            if (CattoWorld.bagFull(cat)) {
                Plan unload = deliver(cat, s -> true, false, 0);
                if (unload != null) {
                    return unload;
                }
            }
            int tier = cat.tier(CattoSkill.MINER);
            boolean nose = cat.moduleActive(CattoModule.KEEN_NOSE);
            int radius = (tier >= 3 ? 32 : tier == 2 ? 24 : 16) + (nose ? 8 : 0);
            CattoScanner scan = scanner(brain, "mine", () -> CattoScanner.around("mine", task.a, radius, Math.min(radius, 16),
                    (p, st) -> st.is(block), brain.now()));
            if (!scan.complete()) {
                scan.tick(level, CattoScanner.BUDGET);
                return Plan.BUSY;
            }
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            boolean toolProblem = false;
            boolean tierProblem = false;
            for (BlockPos p : scan.found) {
                if (brain.blacklisted(p) || !level.isLoaded(p)) {
                    continue;
                }
                BlockState st = level.getBlockState(p);
                if (!st.is(block)) {
                    continue;
                }
                if (!CattoWorld.minerAllows(st, tier)) {
                    tierProblem = true;
                    continue;
                }
                if (!CattoWorld.toolHarvests(cat, st)) {
                    toolProblem = true;
                    continue;
                }
                if (!CattoWorld.safeToBreak(cat, p)) {
                    continue;
                }
                boolean open = CattoWorld.exposed(level, p);
                if (!open && !nose) {
                    continue;
                }
                double d = p.distSqr(cat.blockPosition()) + (open ? 0 : 64);
                if (d < bestDist) {
                    bestDist = d;
                    best = p;
                }
            }
            if (best == null) {
                if (toolProblem) {
                    return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.need_tool", block.getName()));
                }
                if (tierProblem) {
                    return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.miner_tier", block.getName()));
                }
                if (brain.now() - scan.startedAt > 1200 && !brain.memo.containsKey("rescanned")) {
                    brain.memo.put("rescanned", true);
                    brain.scanner = null;
                    return Plan.BUSY;
                }
                cat.report(false, "arcane_artifacts.catto.report.none_left", "#" + block.getDescriptionId());
                task.phase = 1;
            } else {
                BlockPos target = CattoWorld.exposed(level, best) ? best : tunnel(cat, best);
                if (target == null) {
                    brain.blacklist(best, 1200);
                    return Plan.BUSY;
                }
                Step s = step(Step.Kind.BREAK, target, 3.0, CattoWorld.breakTicks(cat, level.getBlockState(target), target));
                return Plan.step(s);
            }
        }
        Plan unload = deliver(cat, s -> true, false, 0);
        return unload != null ? unload : Plan.DONE;
    }

    /** Keen Nose: the first solid block on the straight line from Catto to a buried target (dig toward it). */
    @Nullable
    private static BlockPos tunnel(Catto cat, BlockPos target) {
        Level level = cat.level();
        Vec3 from = cat.position().add(0, 0.35, 0);
        Vec3 to = Vec3.atCenterOf(target);
        int steps = (int) Math.ceil(from.distanceTo(to) * 2);
        int tier = cat.tier(CattoSkill.MINER);
        for (int i = 1; i <= steps; i++) {
            BlockPos p = BlockPos.containing(from.lerp(to, i / (double) steps));
            if (p.equals(target)) {
                return target;
            }
            BlockState st = level.getBlockState(p);
            if (st.isAir() || st.canBeReplaced() && level.getFluidState(p).isEmpty()) {
                continue;
            }
            boolean ok = st.getDestroySpeed(level, p) >= 0 && CattoWorld.minerAllows(st, tier) && CattoWorld.toolHarvests(cat, st)
                    && CattoWorld.safeToBreak(cat, p) && level.getBlockEntity(p) == null;
            return ok ? p : null;
        }
        return target;
    }

    // ------------------------------------------------------------------ collect

    private static AABB area(Catto cat, CattoTask task) {
        if (task.b != null) {
            BlockPos min = BlockPos.min(task.a, task.b);
            BlockPos max = BlockPos.max(task.a, task.b);
            return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        }
        int tier = cat.tier(CattoSkill.GATHERER);
        int radius = (tier >= 3 ? 16 : tier == 2 ? 12 : 8) + (cat.moduleActive(CattoModule.MAGNET_WHISKER) ? 4 : 0);
        return new AABB(task.a).inflate(radius, 4, radius);
    }

    /** Ripe plants Catto may harvest at this Gatherer tier. */
    static boolean ripe(Level level, BlockPos pos, BlockState state, int tier) {
        Block b = state.getBlock();
        if (tier < 2) {
            return false;
        }
        if (b instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (b instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        if (b instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (tier < 3) {
            return false;
        }
        if (b instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        if (b == Blocks.MELON || b == Blocks.PUMPKIN) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(pos.relative(dir)).getBlock() instanceof AttachedStemBlock) {
                    return true;
                }
            }
            return false;
        }
        if (b == Blocks.SUGAR_CANE || b == Blocks.BAMBOO) {
            return level.getBlockState(pos.below()).is(b);
        }
        return false;
    }

    private static Plan collect(Catto cat, CattoBrain brain, CattoTask task) {
        Level level = cat.level();
        Predicate<ItemStack> filter = task.item.isEmpty() ? s -> true : s -> ItemStack.isSameItem(s, task.item);
        if (task.phase == 0 && task.count > 0 && task.done >= task.count) {
            task.phase = 1;
        }
        if (task.phase == 0) {
            if (CattoWorld.bagFull(cat)) {
                Plan unload = deliver(cat, s -> true, false, 0);
                if (unload != null) {
                    return unload;
                }
            }
            AABB box = area(cat, task);
            ItemEntity nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && filter.test(e.getItem()))) {
                if (brain.blacklisted(item.blockPosition())) {
                    continue;
                }
                double d = item.distanceToSqr(cat);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = item;
                }
            }
            if (nearest != null) {
                Step s = step(Step.Kind.PICKUP, nearest.blockPosition(), 1.3, 2);
                s.entity = nearest;
                s.accuracy = 0;
                return Plan.step(s);
            }
            int tier = cat.tier(CattoSkill.GATHERER);
            if (tier >= 2 && task.item.isEmpty()) {
                BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
                BlockPos max = BlockPos.containing(box.maxX - 1, box.maxY - 1, box.maxZ - 1);
                CattoScanner scan = scanner(brain, "crops", () -> new CattoScanner("crops", min, max,
                        (p, st) -> ripe(level, p, st, tier), brain.now()));
                if (!scan.complete()) {
                    scan.tick(level, CattoScanner.BUDGET);
                    return Plan.BUSY;
                }
                BlockPos best = null;
                double bestCrop = Double.MAX_VALUE;
                for (BlockPos p : scan.found) {
                    if (brain.blacklisted(p) || !ripe(level, p, level.getBlockState(p), tier)) {
                        continue;
                    }
                    double d = p.distSqr(cat.blockPosition());
                    if (d < bestCrop) {
                        bestCrop = d;
                        best = p;
                    }
                }
                if (best != null) {
                    return Plan.step(step(Step.Kind.HARVEST, best, 2.5, 10));
                }
            }
            task.phase = 1;
        }
        Plan unload = deliver(cat, s -> true, false, 0);
        return unload != null ? unload : Plan.DONE;
    }

    // ------------------------------------------------------------------ fetch

    private static Plan fetch(Catto cat, CattoTask task) {
        Predicate<ItemStack> want = s -> ItemStack.isSameItem(s, task.item);
        int need = task.count - task.done;
        if (need <= 0) {
            return Plan.DONE;
        }
        int have = CattoWorld.bagCount(cat, want);
        if (task.phase == 0) {
            if (have >= need) {
                task.phase = 1;
            } else {
                BlockPos source = CattoWorld.storageWith(cat, want);
                if (source != null && !CattoWorld.bagFull(cat)) {
                    Step s = step(Step.Kind.TAKE, source, 2.5, 8);
                    s.filter = want;
                    s.amount = need - have;
                    return Plan.step(s);
                }
                if (have == 0) {
                    return Plan.blocked(Component.translatable(cat.storages.isEmpty()
                            ? "arcane_artifacts.catto.blocked.no_storages" : "arcane_artifacts.catto.blocked.not_found", task.item.getHoverName()));
                }
                cat.report(true, "arcane_artifacts.catto.report.partial", "#" + task.item.getDescriptionId(), String.valueOf(have), String.valueOf(need));
                task.phase = 1;
            }
        }
        if (have == 0) {
            return Plan.DONE;
        }
        ServerPlayer owner = cat.ownerPlayer();
        if (owner == null || owner.level() != cat.level()) {
            return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.owner_far"));
        }
        Step s = step(Step.Kind.GIVE, owner.blockPosition(), 2.0, 6);
        s.entity = owner;
        s.filter = want;
        s.amount = need;
        s.blacklistOnFail = false;
        return Plan.step(s);
    }

    // ------------------------------------------------------------------ craft

    /** Does a crafting recipe fit Catto's Crafter tier? Tier I: 2x2 only. */
    private static boolean fits(CraftingRecipe recipe, int tier) {
        if (tier >= 2) {
            return true;
        }
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getWidth() <= 2 && shaped.getHeight() <= 2;
        }
        return ingredients(recipe).size() <= 4;
    }

    private static List<Ingredient> ingredients(CraftingRecipe recipe) {
        List<Ingredient> out = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty()) {
                out.add(ingredient);
            }
        }
        return out;
    }

    /** The crafting recipe for {@code target} that Catto can do and has the most ingredients for. */
    @Nullable
    public static RecipeHolder<CraftingRecipe> findRecipe(Catto cat, ItemStack target) {
        Level level = cat.level();
        int tier = cat.tier(CattoSkill.CRAFTER);
        RecipeHolder<CraftingRecipe> best = null;
        int bestMissing = Integer.MAX_VALUE;
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            if (recipe.isSpecial()) {
                continue;
            }
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty() || !ItemStack.isSameItem(result, target) || !fits(recipe, tier)) {
                continue;
            }
            List<Ingredient> ings = ingredients(recipe);
            if (ings.isEmpty()) {
                continue;
            }
            int missing = 0;
            for (Ingredient ing : ings) {
                if (CattoWorld.bagCount(cat, ing) == 0 && CattoWorld.storageCount(cat, ing) == 0) {
                    missing++;
                }
            }
            if (missing < bestMissing) {
                bestMissing = missing;
                best = holder;
            }
        }
        return best;
    }

    /** Ingredients of one craft that the bag does not cover. */
    private static List<Ingredient> missing(Catto cat, List<Ingredient> ings) {
        int[] counts = new int[cat.capacity()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = cat.bag.getStackInSlot(i).getCount();
        }
        List<Ingredient> missing = new ArrayList<>();
        outer:
        for (Ingredient ing : ings) {
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > 0 && ing.test(cat.bag.getStackInSlot(i))) {
                    counts[i]--;
                    continue outer;
                }
            }
            missing.add(ing);
        }
        return missing;
    }

    @SuppressWarnings("unchecked")
    private static Plan craft(Catto cat, CattoBrain brain, CattoTask task, Map<String, Object> memo) {
        Level level = cat.level();
        RecipeHolder<CraftingRecipe> holder = (RecipeHolder<CraftingRecipe>) memo.computeIfAbsent("recipe", k -> findRecipe(cat, task.item));
        if (holder == null) {
            memo.remove("recipe");
            return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.no_recipe", task.item.getHoverName()));
        }
        ItemStack result = holder.value().getResultItem(level.registryAccess());
        Predicate<ItemStack> isResult = s -> ItemStack.isSameItem(s, result);
        if (task.phase == 0 && task.done >= task.count) {
            task.phase = 1;
        }
        if (task.phase == 0) {
            List<Ingredient> ings = ingredients(holder.value());
            List<Ingredient> missing = missing(cat, ings);
            if (missing.isEmpty()) {
                if (!CattoWorld.bagFits(cat, result)) {
                    Plan unload = deliver(cat, isResult, true, 0);
                    if (unload != null) {
                        return unload;
                    }
                    return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.bag_full"));
                }
                if (cat.workItem().is(Items.CRAFTING_TABLE)) {
                    Step s = step(Step.Kind.CRAFT, cat.blockPosition(), 64.0, craftTicks(cat));
                    s.blacklistOnFail = false;
                    return Plan.step(s);
                }
                BlockPos table = memo.get("table") instanceof BlockPos p && level.getBlockState(p).is(Blocks.CRAFTING_TABLE) ? p : null;
                if (table == null) {
                    CattoScanner scan = scanner(brain, "table", () -> CattoScanner.around("table", cat.blockPosition(), 16, 6,
                            (p, st) -> st.is(Blocks.CRAFTING_TABLE), brain.now()));
                    if (!scan.complete()) {
                        scan.tick(level, CattoScanner.BUDGET);
                        return Plan.BUSY;
                    }
                    double bestDist = Double.MAX_VALUE;
                    for (BlockPos p : scan.found) {
                        if (level.getBlockState(p).is(Blocks.CRAFTING_TABLE) && !brain.blacklisted(p) && p.distSqr(cat.blockPosition()) < bestDist) {
                            bestDist = p.distSqr(cat.blockPosition());
                            table = p;
                        }
                    }
                    brain.scanner = null;
                    if (table == null) {
                        return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.no_table"));
                    }
                    memo.put("table", table);
                }
                return Plan.step(step(Step.Kind.CRAFT, table, 2.5, craftTicks(cat)));
            }
            Ingredient first = missing.get(0);
            int amount = 0;
            for (Ingredient ing : missing) {
                if (ing == first || ing.equals(first)) {
                    amount++;
                }
            }
            amount *= Math.max(1, Math.min(8, (task.count - task.done + result.getCount() - 1) / Math.max(1, result.getCount())));
            BlockPos source = CattoWorld.storageWith(cat, first);
            if (source != null && !CattoWorld.bagFull(cat)) {
                Step s = step(Step.Kind.TAKE, source, 2.5, 8);
                s.filter = first;
                s.amount = amount;
                return Plan.step(s);
            }
            ItemStack[] options = first.getItems();
            if (cat.tier(CattoSkill.CRAFTER) >= 3 && !task.sub && options.length > 0 && findRecipe(cat, options[0]) != null) {
                CattoTask sub = CattoTask.craft(options[0], amount);
                sub.sub = true;
                cat.queue.add(0, sub);
                cat.report(false, "arcane_artifacts.catto.report.sub_craft", "#" + options[0].getDescriptionId());
                cat.changed();
                return Plan.BUSY;
            }
            return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.missing",
                    options.length > 0 ? options[0].getHoverName() : Component.literal("?")));
        }
        Plan unload = deliver(cat, isResult, true, 0);
        return unload != null ? unload : Plan.DONE;
    }

    private static int craftTicks(Catto cat) {
        return switch (cat.tier(CattoSkill.CRAFTER)) {
            case 3 -> 10;
            case 2 -> 20;
            default -> 30;
        };
    }

    // ------------------------------------------------------------------ build

    @SuppressWarnings("unchecked")
    private static Plan build(Catto cat, CattoBrain brain, CattoTask task, Map<String, Object> memo) {
        Level level = cat.level();
        List<Map.Entry<BlockPos, BlockState>> layout = (List<Map.Entry<BlockPos, BlockState>>) memo.computeIfAbsent("layout", k -> layout(cat, task));
        if (layout.isEmpty()) {
            return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.bad_build"));
        }
        int tier = cat.tier(CattoSkill.BUILDER);
        int cursor = (int) memo.getOrDefault("cursor", 0);
        int done = 0;
        boolean waiting = false;
        BlockPos min = null;
        for (int i = 0; i < layout.size(); i++) {
            BlockPos pos = layout.get(i).getKey();
            BlockState target = layout.get(i).getValue();
            min = min == null ? pos : BlockPos.min(min, pos);
            BlockState current = level.getBlockState(pos);
            if (current.equals(target) || task.blueprint == null && current.is(target.getBlock())) {
                done++;
                if (i == cursor) {
                    cursor++;
                }
                continue;
            }
            if (i < cursor) {
                cursor = i;
            }
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (brain.blacklisted(pos)) {
                waiting = true;
                continue;
            }
            if (!current.canBeReplaced()) {
                if (tier >= 3 && current.getDestroySpeed(level, pos) >= 0 && CattoWorld.safeToBreak(cat, pos)
                        && CattoWorld.toolHarvests(cat, current) && CattoWorld.minerAllows(current, cat.tier(CattoSkill.MINER))) {
                    memo.put("cursor", cursor);
                    task.done = done;
                    return Plan.step(step(Step.Kind.BREAK, pos, 3.5, CattoWorld.breakTicks(cat, current, pos)));
                }
                continue;
            }
            Item item = target.getBlock().asItem();
            Predicate<ItemStack> isItem = s -> s.is(item);
            memo.put("cursor", cursor);
            task.done = done;
            if (CattoWorld.bagCount(cat, isItem) > 0) {
                Step s = step(Step.Kind.PLACE, pos, 3.5, tier >= 3 ? 3 : tier == 2 ? 5 : 8);
                s.state = target;
                return Plan.step(s);
            }
            BlockPos source = CattoWorld.storageWith(cat, isItem);
            if (source != null && !CattoWorld.bagFull(cat)) {
                Step s = step(Step.Kind.TAKE, source, 2.5, 8);
                s.filter = isItem;
                s.amount = 64;
                return Plan.step(s);
            }
            return Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.no_material", new ItemStack(item).getHoverName()));
        }
        task.done = done;
        memo.put("cursor", cursor);
        if (waiting) {
            // Something (often Catto itself) stands where a block goes: step aside and try again.
            if (!memo.containsKey("nudged") && min != null) {
                memo.put("nudged", true);
                Step s = step(Step.Kind.GOTO, min.offset(-2, 0, -2), 1.5, 20);
                s.blacklistOnFail = false;
                return Plan.step(s);
            }
            return Plan.BUSY;
        }
        return Plan.DONE;
    }

    private static List<Map.Entry<BlockPos, BlockState>> layout(Catto cat, CattoTask task) {
        if (task.blueprint != null) {
            return CattoItems.BlueprintItem.layout(cat.level(), task.blueprint, task.a);
        }
        List<Map.Entry<BlockPos, BlockState>> out = new ArrayList<>();
        if (task.b == null || !(task.item.getItem() instanceof BlockItem blockItem)) {
            return out;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        BlockPos min = BlockPos.min(task.a, task.b);
        BlockPos max = BlockPos.max(task.a, task.b);
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    out.add(Map.entry(new BlockPos(x, y, z), state));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ explore

    @SuppressWarnings("unchecked")
    private static Plan explore(Catto cat, CattoBrain brain, CattoTask task, Map<String, Object> memo) {
        List<BlockPos> waypoints = (List<BlockPos>) memo.computeIfAbsent("waypoints", k -> waypoints(cat, task));
        while (task.waypoint < waypoints.size()) {
            BlockPos wp = waypoints.get(task.waypoint);
            if (brain.blacklisted(wp) || !cat.level().isLoaded(wp)) {
                task.waypoint++;
                continue;
            }
            Step s = step(Step.Kind.GOTO, wp, 4.0, 30);
            s.accuracy = 3;
            return Plan.step(s);
        }
        return Plan.DONE;
    }

    private static List<BlockPos> waypoints(Catto cat, CattoTask task) {
        Level level = cat.level();
        int tier = cat.tier(CattoSkill.EXPLORER);
        int radius = tier >= 3 ? 96 : tier == 2 ? 64 : 32;
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, task.a.getX(), task.a.getZ());
        boolean underground = task.a.getY() < surface - 6;
        List<BlockPos> out = new ArrayList<>();
        out.add(task.a);
        for (int ring = 1; ring <= tier; ring++) {
            double r = radius * ring / (double) tier;
            int n = 6 * ring;
            for (int j = 0; j < n; j++) {
                double angle = Math.PI * 2 * j / n;
                int x = task.a.getX() + (int) Math.round(Math.cos(angle) * r);
                int z = task.a.getZ() + (int) Math.round(Math.sin(angle) * r);
                BlockPos column = new BlockPos(x, task.a.getY(), z);
                if (!level.isLoaded(column)) {
                    continue;
                }
                int y = underground ? task.a.getY() : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                out.add(new BlockPos(x, y, z));
            }
        }
        return out;
    }

    static boolean interesting(BlockState state, int tier) {
        if (state.is(Tags.Blocks.ORES)) {
            return true;
        }
        if (tier < 3) {
            return false;
        }
        return state.is(Blocks.SPAWNER) || state.is(Blocks.CHEST) || state.is(Blocks.BARREL) || state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.ANCIENT_DEBRIS) || state.is(BlockTags.SHULKER_BOXES);
    }

    @SuppressWarnings("unchecked")
    private static void lookAround(Catto cat, CattoBrain brain, CattoTask task) {
        Level level = cat.level();
        int tier = cat.tier(CattoSkill.EXPLORER);
        int r = (tier >= 3 ? 12 : 8) + (cat.moduleActive(CattoModule.KEEN_NOSE) ? 4 : 0);
        Set<Long> seen = (Set<Long>) brain.memo.computeIfAbsent("seen", k -> new HashSet<Long>());
        BlockPos centre = cat.blockPosition();
        int newKinds = 0;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            if (!level.isLoaded(p)) {
                continue;
            }
            BlockState st = level.getBlockState(p);
            if (!interesting(st, tier) || !seen.add(p.asLong())) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
            if (!task.found.containsKey(id)) {
                newKinds++;
            }
            task.found.merge(id, 1, Integer::sum);
            BlockPos old = task.nearest.get(id);
            if (old == null || p.distSqr(task.a) < old.distSqr(task.a)) {
                task.nearest.put(id, p.immutable());
            }
        }
        task.waypoint++;
        task.done = task.waypoint;
        cat.addXp(CattoSkill.EXPLORER, 2 + newKinds);
        cat.changed();
    }

    /** At the end of exploring: the findings go to the report log and to the owner's chat. */
    static void reportFindings(Catto cat, CattoTask task) {
        int tier = cat.tier(CattoSkill.EXPLORER);
        ServerPlayer owner = cat.ownerPlayer();
        if (task.found.isEmpty()) {
            cat.report(false, "arcane_artifacts.catto.report.found_nothing");
            return;
        }
        List<Map.Entry<ResourceLocation, Integer>> sorted = new ArrayList<>(task.found.entrySet());
        sorted.sort((x, y) -> y.getValue() - x.getValue());
        int shown = 0;
        for (Map.Entry<ResourceLocation, Integer> e : sorted) {
            if (shown++ >= 8) {
                break;
            }
            Block block = BuiltInRegistries.BLOCK.get(e.getKey());
            BlockPos at = task.nearest.get(e.getKey());
            String place = at == null ? "?" : at.getX() + " " + at.getY() + " " + at.getZ();
            if (tier >= 2) {
                cat.report(false, "arcane_artifacts.catto.report.found_at", "#" + block.getDescriptionId(), String.valueOf(e.getValue()), place);
            } else {
                cat.report(false, "arcane_artifacts.catto.report.found", "#" + block.getDescriptionId(), String.valueOf(e.getValue()));
            }
            if (owner != null) {
                owner.sendSystemMessage(tier >= 2
                        ? Component.translatable("arcane_artifacts.catto.report.found_at", block.getName(), e.getValue(), place)
                        : Component.translatable("arcane_artifacts.catto.report.found", block.getName(), e.getValue()));
            }
        }
    }

    // ------------------------------------------------------------------ doing a step

    @SuppressWarnings("unchecked")
    static boolean perform(Catto cat, CattoBrain brain, CattoTask task, Step step) {
        ServerLevel level = (ServerLevel) cat.level();
        switch (step.kind) {
            case GOTO -> {
                if (task.type == CattoTask.Type.EXPLORE) {
                    lookAround(cat, brain, task);
                }
                return true;
            }
            case BREAK -> {
                BlockState before = level.getBlockState(step.pos);
                Block target = task.block == null ? null : BuiltInRegistries.BLOCK.get(task.block);
                if (!CattoWorld.toolHarvests(cat, before) || !CattoWorld.safeToBreak(cat, step.pos) || !CattoWorld.breakBlock(cat, step.pos)) {
                    return false;
                }
                CattoWorld.vacuum(cat, step.pos, cat.moduleActive(CattoModule.MAGNET_WHISKER) ? 4.0 : 1.5, s -> true);
                if (task.type == CattoTask.Type.MINE) {
                    if (target != null && before.is(target)) {
                        task.done++;
                    }
                    cat.addXp(CattoSkill.MINER, 1);
                }
                return true;
            }
            case PLACE -> {
                if (step.state == null || !CattoWorld.place(cat, step.pos, step.state)) {
                    return false;
                }
                task.done++;
                cat.addXp(CattoSkill.BUILDER, 1);
                return true;
            }
            case PICKUP -> {
                if (!(step.entity instanceof ItemEntity item) || !item.isAlive()) {
                    return false;
                }
                int picked = CattoWorld.pickUp(cat, item);
                task.done += picked;
                cat.addXp(CattoSkill.GATHERER, picked > 0 ? 1 : 0);
                return picked > 0;
            }
            case HARVEST -> {
                return harvest(cat, task, step.pos);
            }
            case TAKE -> {
                return CattoWorld.takeFrom(cat, step.pos, step.filter, step.amount) > 0;
            }
            case PUT -> {
                return CattoWorld.putInto(cat, step.pos, step.filter) > 0;
            }
            case GIVE -> {
                if (!(step.entity instanceof ServerPlayer owner) || !owner.isAlive()) {
                    return false;
                }
                int given = give(cat, owner, step.filter, step.amount);
                if (task.type == CattoTask.Type.FETCH) {
                    task.done += given;
                }
                return given > 0;
            }
            case CRAFT -> {
                return craftOnce(cat, brain, task);
            }
        }
        return false;
    }

    private static int give(Catto cat, ServerPlayer owner, Predicate<ItemStack> filter, int amount) {
        int given = 0;
        int limit = amount <= 0 ? Integer.MAX_VALUE : amount;
        for (int i = 0; i < cat.capacity() && given < limit; i++) {
            ItemStack stack = cat.bag.getStackInSlot(i);
            if (stack.isEmpty() || !filter.test(stack)) {
                continue;
            }
            ItemStack out = cat.bag.extractItem(i, Math.min(limit - given, stack.getCount()), false);
            given += out.getCount();
            if (!owner.getInventory().add(out)) {
                owner.drop(out, false);
            }
        }
        if (given > 0) {
            cat.level().playSound(null, owner.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
        }
        return given;
    }

    private static boolean harvest(Catto cat, CattoTask task, BlockPos pos) {
        ServerLevel level = (ServerLevel) cat.level();
        BlockState state = level.getBlockState(pos);
        int tier = cat.tier(CattoSkill.GATHERER);
        if (!ripe(level, pos, state, tier)) {
            return false;
        }
        Block block = state.getBlock();
        int picked;
        if (block instanceof SweetBerryBushBlock) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            int n = 1 + cat.getRandom().nextInt(2) + (age == 3 ? 1 : 0);
            level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
            Block.popResource(level, pos, new ItemStack(Items.SWEET_BERRIES, n));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
            picked = CattoWorld.vacuum(cat, pos, 1.5, s -> true);
        } else {
            if (!CattoWorld.breakBlock(cat, pos)) {
                return false;
            }
            picked = CattoWorld.vacuum(cat, pos, 1.5, s -> true);
            // Replant what grows from seeds; cocoa keeps its side of the log.
            if (block instanceof CropBlock || block instanceof NetherWartBlock || block instanceof CocoaBlock) {
                BlockState replant = block instanceof CocoaBlock ? state.setValue(CocoaBlock.AGE, 0) : block.defaultBlockState();
                CattoWorld.place(cat, pos, replant);
            }
        }
        task.done += picked;
        cat.addXp(CattoSkill.GATHERER, 1);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean craftOnce(Catto cat, CattoBrain brain, CattoTask task) {
        Object stored = brain.memo.get("recipe");
        if (!(stored instanceof RecipeHolder<?> raw)) {
            return false;
        }
        RecipeHolder<CraftingRecipe> holder = (RecipeHolder<CraftingRecipe>) raw;
        List<Ingredient> ings = ingredients(holder.value());
        if (!missing(cat, ings).isEmpty()) {
            return false;
        }
        List<ItemStack> leftovers = new ArrayList<>();
        for (Ingredient ing : ings) {
            for (int i = 0; i < cat.capacity(); i++) {
                ItemStack stack = cat.bag.getStackInSlot(i);
                if (!stack.isEmpty() && ing.test(stack)) {
                    ItemStack used = cat.bag.extractItem(i, 1, false);
                    if (used.hasCraftingRemainingItem()) {
                        leftovers.add(used.getCraftingRemainingItem());
                    }
                    break;
                }
            }
        }
        ItemStack result = holder.value().getResultItem(cat.level().registryAccess()).copy();
        task.done += result.getCount();
        leftovers.add(result);
        for (ItemStack stack : leftovers) {
            ItemStack rest = CattoWorld.bagInsert(cat, stack);
            if (!rest.isEmpty()) {
                cat.spawnAtLocation(rest);
            }
        }
        cat.addXp(CattoSkill.CRAFTER, 2);
        cat.level().playSound(null, cat.blockPosition(), SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.NEUTRAL, 0.5F, 1.5F);
        return true;
    }
}
