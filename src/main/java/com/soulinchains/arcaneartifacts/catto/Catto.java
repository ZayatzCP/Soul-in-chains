package com.soulinchains.arcaneartifacts.catto;

import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Catto, the arcane cat familiar. A tamed cat bound by the Familiar Bond ritual: it keeps its coat, follows its owner and
 * carries out errands (craft, mine, gather, fetch, build, explore) from a queue. All decisions are made on the server by
 * {@link CattoBrain}; the client only draws the cat, its bubble and the menu.
 */
public class Catto extends Cat {
    public static final int SLOT_HEAD = 0;
    public static final int SLOT_ARMOR = 1;
    public static final int SLOT_BAG = 2;
    public static final int SLOT_TOOL = 3;
    public static final int SLOT_WEAPON = 4;
    public static final int SLOT_OFFHAND = 5;
    public static final int EQUIPMENT_SIZE = 6;
    public static final int BAG_SIZE = 27;
    public static final int MOUTH_SLOTS = 3;
    public static final int MAX_STORAGES = 8;
    public static final int MAX_REPORTS = 20;
    public static final int BASE_QUEUE = 8;
    public static final int RECALL_COST = 50;
    public static final int WORK_RADIUS = 48;

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Catto.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Component> DATA_STATUS = SynchedEntityData.defineId(Catto.class, EntityDataSerializers.COMPONENT);
    private static final EntityDataAccessor<ItemStack> DATA_ICON = SynchedEntityData.defineId(Catto.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_SOURCE = SynchedEntityData.defineId(Catto.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_SOURCE = SynchedEntityData.defineId(Catto.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_COAT = SynchedEntityData.defineId(Catto.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Component> DATA_SPEECH = SynchedEntityData.defineId(Catto.class, EntityDataSerializers.COMPONENT);
    private static final EntityDataAccessor<Boolean> DATA_ALERT = SynchedEntityData.defineId(Catto.class, EntityDataSerializers.BOOLEAN);

    /** Headwear, armor, bag, tool, weapon and the second paw (food, shield, torches). */
    public final ItemStackHandler equipment = new ItemStackHandler(EQUIPMENT_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return Catto.this.fitsSlot(slot, stack);
        }
    };
    /** The bag: only the first {@link #capacity()} slots can be used. */
    public final ItemStackHandler bag = new ItemStackHandler(BAG_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    public final List<CattoTask> queue = new ArrayList<>();
    public final List<BlockPos> storages = new ArrayList<>();
    public final ArrayDeque<Report> reports = new ArrayDeque<>();
    public final int[] xp = new int[CattoSkill.values().length];
    public boolean paused;
    public boolean stay;
    public int bond;
    public int source = 100;
    /** Temperament: chosen once, when this Catto first appears. */
    public CattoNature nature;
    /** Standing order: guard a spot or escort the owner (the queue waits meanwhile). */
    public CattoMode mode = CattoMode.NONE;
    @Nullable
    public BlockPos guardPos;
    /** A village Catto's cushion: it stays around it. */
    @Nullable
    public BlockPos homePos;
    /** Talking to other Cattos and humming tunes. */
    public final CattoChatter chatter = new CattoChatter(this);
    private int speechTicks;
    private long lastPetTime = -100000;
    /** Game tests: works without an online owner. Never saved. */
    public boolean testMode;
    /** Bumped on every change the owner's screen should see. */
    public int revision;
    @Nullable
    private CattoBrain brain;

    public Catto(EntityType<? extends Cat> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        nature = CattoNature.random(getRandom());
        entityData.set(DATA_COAT, (byte) CattoCoat.random(getRandom()).ordinal());
        for (CattoSkill skill : CattoSkill.values()) {
            xp[skill.ordinal()] = nature.startXp(skill);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Cat.createAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    public CattoBrain brain() {
        if (brain == null) {
            brain = new CattoBrain(this);
        }
        return brain;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) 0);
        builder.define(DATA_STATUS, Component.empty());
        builder.define(DATA_ICON, ItemStack.EMPTY);
        builder.define(DATA_SOURCE, 0);
        builder.define(DATA_ALERT, false);
        builder.define(DATA_MAX_SOURCE, 500);
        builder.define(DATA_COAT, (byte) 0);
        builder.define(DATA_SPEECH, Component.empty());
    }

    @Override
    protected void registerGoals() {
        // A bound Catto is driven by one brain (it holds movement all the time). A village Catto without an owner
        // only strolls around, watches people and follows someone holding fish.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CattoBrain.Goal(this));
        goalSelector.addGoal(2, new CushionGoal(this));
        goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.TemptGoal(this, 0.7,
                net.minecraft.world.item.crafting.Ingredient.of(Items.COD, Items.SALMON), false));
        goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
    }

    /** A village Catto goes back to its cushion now and then and sits on it for a while. */
    static final class CushionGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final Catto cat;
        private int sitTicks;

        CushionGoal(Catto cat) {
            this.cat = cat;
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return cat.getOwnerUUID() == null && cat.homePos != null && cat.getRandom().nextInt(200) == 0
                    && cat.homePos.distSqr(cat.blockPosition()) < 24 * 24;
        }

        @Override
        public void start() {
            sitTicks = 300 + cat.getRandom().nextInt(900);
            cat.getNavigation().moveTo(cat.homePos.getX() + 0.5, cat.homePos.getY(), cat.homePos.getZ() + 0.5, 0.8);
        }

        @Override
        public boolean canContinueToUse() {
            return sitTicks > 0 && cat.getOwnerUUID() == null;
        }

        @Override
        public void tick() {
            if (cat.homePos != null && cat.position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(cat.homePos)) < 1.5) {
                cat.getNavigation().stop();
                cat.setInSittingPose(true);
                sitTicks--;
            } else if (cat.getNavigation().isDone()) {
                sitTicks -= 20;
            }
        }

        @Override
        public void stop() {
            cat.setInSittingPose(false);
        }
    }

    // ------------------------------------------------------------------ synced state

    public CattoBrain.State shownState() {
        CattoBrain.State[] states = CattoBrain.State.values();
        int i = entityData.get(DATA_STATE);
        return states[Math.floorMod(i, states.length)];
    }

    public Component status() {
        return entityData.get(DATA_STATUS);
    }

    public ItemStack bubble() {
        return entityData.get(DATA_ICON);
    }

    public int shownSource() {
        return entityData.get(DATA_SOURCE);
    }

    public int shownMaxSource() {
        return entityData.get(DATA_MAX_SOURCE);
    }

    public CattoCoat coat() {
        CattoCoat[] coats = CattoCoat.values();
        return coats[Math.floorMod(entityData.get(DATA_COAT), coats.length)];
    }

    public void setCoat(CattoCoat coat) {
        entityData.set(DATA_COAT, (byte) coat.ordinal());
    }

    /** A line over Catto's head (conversation, song, remark). */
    public Component speech() {
        return entityData.get(DATA_SPEECH);
    }

    public void say(Component line, int ticks) {
        entityData.set(DATA_SPEECH, line);
        speechTicks = ticks;
    }

    public boolean alert() {
        return entityData.get(DATA_ALERT);
    }

    void show(CattoBrain.State state, Component status, ItemStack icon, boolean alert) {
        if (entityData.get(DATA_STATE) != (byte) state.ordinal()) {
            entityData.set(DATA_STATE, (byte) state.ordinal());
        }
        if (!entityData.get(DATA_STATUS).equals(status)) {
            entityData.set(DATA_STATUS, status);
            changed();
        }
        if (!ItemStack.matches(entityData.get(DATA_ICON), icon)) {
            entityData.set(DATA_ICON, icon.copy());
        }
        if (entityData.get(DATA_ALERT) != alert) {
            entityData.set(DATA_ALERT, alert);
        }
    }

    void changed() {
        revision++;
    }

    // ------------------------------------------------------------------ skills, bond, source

    public int tier(CattoSkill skill) {
        return CattoSkill.tier(xp[skill.ordinal()]);
    }

    public void addXp(CattoSkill skill, int amount) {
        if (amount <= 0) {
            return;
        }
        int before = tier(skill);
        xp[skill.ordinal()] += amount;
        int after = tier(skill);
        if (after > before) {
            report(false, "arcane_artifacts.catto.report.tier_up", "#arcane_artifacts.catto.skill." + skill.key(), String.valueOf(after));
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.4, 0.3, 0.2);
                level.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.6F);
            }
        }
        changed();
    }

    /** Source pool: the Alchemist's tier plus what the headwear and armor add. */
    public int maxSource() {
        int base = switch (tier(CattoSkill.ALCHEMIST)) {
            case 3 -> 2000;
            case 2 -> 1000;
            default -> 500;
        };
        for (int slot : new int[]{SLOT_HEAD, SLOT_ARMOR}) {
            if (equipment.getStackInSlot(slot).getItem() instanceof CattoGear.GearItem gear) {
                base += gear.mana;
            }
        }
        return base;
    }

    public int armor() {
        int armor = 0;
        for (int slot : new int[]{SLOT_HEAD, SLOT_ARMOR}) {
            ItemStack stack = equipment.getStackInSlot(slot);
            // Vanilla helmets and chestplates protect through the game's own armor (they are worn in Catto's armor slots).
            if (stack.getItem() instanceof CattoGear.GearItem gear) {
                armor += gear.armor;
            }
        }
        return armor;
    }

    /** The set whose headwear and armor Catto wears together, if any. */
    @Nullable
    public CattoGear.Set fullSet() {
        if (equipment.getStackInSlot(SLOT_HEAD).getItem() instanceof CattoGear.GearItem head
                && equipment.getStackInSlot(SLOT_ARMOR).getItem() instanceof CattoGear.GearItem body && head.set == body.set) {
            return head.set;
        }
        return null;
    }

    public CattoNature nature() {
        return nature == null ? CattoNature.GENTLE : nature;
    }

    public void addSource(int amount) {
        source = Math.max(0, Math.min(maxSource(), source + amount));
        entityData.set(DATA_SOURCE, source);
    }

    public boolean spendSource(int amount) {
        if (source < amount) {
            return false;
        }
        addSource(-amount);
        return true;
    }

    public int recallCost() {
        return tier(CattoSkill.ALCHEMIST) >= 3 ? RECALL_COST / 2 : RECALL_COST;
    }

    public void addBond(int amount) {
        bond = Math.max(0, Math.min(100, bond + amount));
        changed();
    }

    public int queueCap() {
        return BASE_QUEUE + (bond >= 50 ? 2 : 0) + (bond >= 100 ? 2 : 0);
    }

    // ------------------------------------------------------------------ equipment

    public ItemStack tool() {
        return equipment.getStackInSlot(SLOT_TOOL);
    }

    public ItemStack weapon() {
        return equipment.getStackInSlot(SLOT_WEAPON);
    }

    public ItemStack offhand() {
        return equipment.getStackInSlot(SLOT_OFFHAND);
    }

    public int capacity() {
        return equipment.getStackInSlot(SLOT_BAG).getItem() instanceof CattoItems.PackItem pack ? pack.capacity : MOUTH_SLOTS;
    }

    /** Does the headwear or armor give this effect? */
    public boolean hasModule(CattoModule module) {
        for (int slot : new int[]{SLOT_HEAD, SLOT_ARMOR}) {
            if (equipment.getStackInSlot(slot).getItem() instanceof CattoGear.GearItem gear && gear.effects.contains(module)) {
                return true;
            }
        }
        return false;
    }

    /** A gear effect works only while Catto has Source to feed it. */
    public boolean moduleActive(CattoModule module) {
        return source > 0 && hasModule(module);
    }

    public int activeModules() {
        int n = 0;
        for (CattoModule module : CattoModule.values()) {
            if (moduleActive(module)) {
                n++;
            }
        }
        return n;
    }

    public boolean fitsSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        var item = stack.getItem();
        return switch (slot) {
            case SLOT_HEAD -> item instanceof CattoGear.GearItem gear ? gear.slot == CattoGear.Slot.HEAD
                    : item instanceof net.minecraft.world.item.ArmorItem armor && armor.getType() == net.minecraft.world.item.ArmorItem.Type.HELMET;
            case SLOT_ARMOR -> item instanceof CattoGear.GearItem gear ? gear.slot == CattoGear.Slot.ARMOR
                    : item instanceof net.minecraft.world.item.ArmorItem armor && armor.getType() == net.minecraft.world.item.ArmorItem.Type.CHESTPLATE;
            case SLOT_BAG -> item instanceof CattoItems.PackItem;
            case SLOT_TOOL -> item instanceof net.minecraft.world.item.DiggerItem || item instanceof net.minecraft.world.item.ShearsItem
                    || item instanceof net.minecraft.world.item.FishingRodItem;
            case SLOT_WEAPON -> item instanceof net.minecraft.world.item.SwordItem || item instanceof net.minecraft.world.item.AxeItem
                    || item instanceof net.minecraft.world.item.TridentItem || item instanceof net.minecraft.world.item.MaceItem
                    || CattoCombat.spellOf(stack) != null;
            case SLOT_OFFHAND -> stack.has(net.minecraft.core.component.DataComponents.FOOD) || item instanceof net.minecraft.world.item.ShieldItem
                    || stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH);
            default -> true;
        };
    }

    /** The bag cannot be taken off while it holds things in the slots it gives. */
    public boolean bagRemovable() {
        for (int i = MOUTH_SLOTS; i < BAG_SIZE; i++) {
            if (!bag.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ reports

    /** One line of the report log. Args starting with '#' are translation keys. */
    public record Report(String key, List<String> args, boolean bad) {
        public Component text() {
            Object[] parts = new Object[args.size()];
            for (int i = 0; i < parts.length; i++) {
                String arg = args.get(i);
                parts[i] = arg.startsWith("#") ? Component.translatable(arg.substring(1)) : Component.literal(arg);
            }
            return Component.translatable(key, parts);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Key", key);
            ListTag list = new ListTag();
            args.forEach(a -> list.add(StringTag.valueOf(a)));
            tag.put("Args", list);
            tag.putBoolean("Bad", bad);
            return tag;
        }

        static Report load(CompoundTag tag) {
            List<String> args = new ArrayList<>();
            for (Tag t : tag.getList("Args", Tag.TAG_STRING)) {
                args.add(t.getAsString());
            }
            return new Report(tag.getString("Key"), args, tag.getBoolean("Bad"));
        }
    }

    public void report(boolean bad, String key, String... args) {
        reports.addFirst(new Report(key, List.of(args), bad));
        while (reports.size() > MAX_REPORTS) {
            reports.removeLast();
        }
        changed();
    }

    /** Logs a translatable message so the owner's client translates it (item and block names included). */
    public void report(boolean bad, Component message) {
        if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
            List<String> args = new ArrayList<>();
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component c && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents inner) {
                    args.add("#" + inner.getKey());
                } else if (arg instanceof Component c) {
                    args.add(c.getString());
                } else {
                    args.add(String.valueOf(arg));
                }
            }
            report(bad, translatable.getKey(), args.toArray(new String[0]));
        } else {
            report(bad, "%s", message.getString());
        }
    }

    /** Tells the owner (once per cause) and logs it. */
    public void complain(String key, String... args) {
        report(true, key, args);
        ServerPlayer owner = ownerPlayer();
        if (owner != null) {
            owner.displayClientMessage(Component.translatable("arcane_artifacts.catto.says", getDisplayName(), new Report(key, List.of(args), true).text()), false);
        }
    }

    // ------------------------------------------------------------------ owner

    @Nullable
    public ServerPlayer ownerPlayer() {
        UUID owner = getOwnerUUID();
        if (owner == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(owner);
    }

    public boolean ownerOnline() {
        return testMode || ownerPlayer() != null;
    }

    @Nullable
    public static Catto findById(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof Catto cat && cat.isAlive()) {
                return cat;
            }
        }
        return null;
    }

    /** The player's Catto, if it is alive in a loaded part of some world. */
    @Nullable
    public static Catto find(ServerPlayer player) {
        CattoSavedData.Entry entry = CattoSavedData.get(player.server).entry(player.getUUID());
        if (entry == null || entry.catId == null || entry.status != CattoSavedData.Status.ALIVE) {
            return null;
        }
        return findById(player.server, entry.catId);
    }

    /** The bell rang: Catto drops what it is doing and comes back; far away (same world) it jumps for Source. */
    public static void callFor(ServerPlayer player) {
        CattoSavedData.Entry entry = CattoSavedData.get(player.server).entry(player.getUUID());
        Catto cat = find(player);
        if (cat == null) {
            if (entry == null) {
                CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.call.none"));
            } else if (entry.status == CattoSavedData.Status.FALLEN) {
                CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.call.fallen"));
            } else if (entry.status == CattoSavedData.Status.IN_BELL) {
                CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.call.in_bell"));
            } else {
                CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.call.unloaded", CattoItems.pos(entry.lastPos)));
            }
            return;
        }
        cat.brain().recall(player);
    }

    /** Shift+use with an empty bell: Catto hides inside (everything it has stays with it). */
    public boolean hideIn(ServerPlayer player, ItemStack bell) {
        if (CattoItems.BellItem.holdsCatto(bell) || !(level() instanceof ServerLevel level)) {
            return false;
        }
        CompoundTag tag = saveWithoutId(new CompoundTag());
        CattoItems.edit(bell, t -> t.put("Catto", tag));
        level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.4, getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 0.6F, 1.6F);
        CattoSavedData.get(level.getServer()).hidden(player.getUUID());
        discard();
        CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.bell.hidden"));
        return true;
    }

    /** Use the bell on a block: the Catto inside comes out there. */
    public static boolean release(ServerPlayer player, ServerLevel level, ItemStack bell, BlockPos at) {
        CompoundTag tag = CattoItems.data(bell).getCompound("Catto");
        Catto cat = CattoContent.CATTO.get().create(level);
        if (cat == null || tag.isEmpty()) {
            return false;
        }
        if (!player.getUUID().equals(tag.hasUUID("Owner") ? tag.getUUID("Owner") : null)) {
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.bell.not_yours"));
            return false;
        }
        Catto existing = find(player);
        if (existing != null) {
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.ritual.already"));
            return false;
        }
        cat.load(tag);
        cat.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot() + 180, 0);
        if (level.getEntity(cat.getUUID()) != null) {
            cat.setUUID(UUID.randomUUID());
        }
        level.addFreshEntity(cat);
        CattoSavedData.get(level.getServer()).alive(player.getUUID(), cat);
        level.sendParticles(ParticleTypes.WITCH, cat.getX(), cat.getY() + 0.4, cat.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, at, SoundEvents.CAT_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    // ------------------------------------------------------------------ errands

    /** A new errand from the owner, checked against Catto's skills and the queue size. */
    public boolean offer(ServerPlayer player, CattoTask task) {
        Component refusal = refusal(task);
        if (refusal != null) {
            CattoItems.tell(player, refusal);
            return false;
        }
        queue.add(task);
        changed();
        CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.accepted", task.describe(), queue.size(), queueCap()));
        playSound(SoundEvents.CAT_AMBIENT, 0.6F, 1.2F);
        return true;
    }

    @Nullable
    public Component refusal(CattoTask task) {
        if (queue.size() >= queueCap()) {
            return Component.translatable("arcane_artifacts.catto.refuse.queue_full", queueCap());
        }
        BlockPos at = task.a;
        if (task.type != CattoTask.Type.FETCH && task.type != CattoTask.Type.CRAFT && !testMode
                && ownerPlayer() != null && at.distSqr(ownerPlayer().blockPosition()) > (double) (WORK_RADIUS * WORK_RADIUS) * 4) {
            return Component.translatable("arcane_artifacts.catto.refuse.too_far");
        }
        switch (task.type) {
            case BUILD -> {
                int tier = tier(CattoSkill.BUILDER);
                int limit = tier >= 3 ? 1024 : tier == 2 ? 256 : 64;
                if (task.blueprint != null && tier < 2) {
                    return Component.translatable("arcane_artifacts.catto.refuse.blueprint_tier");
                }
                if (task.count > limit) {
                    return Component.translatable("arcane_artifacts.catto.refuse.build_size", task.count, limit);
                }
            }
            case COLLECT -> {
                if (task.b != null && CattoTask.volume(task.a, task.b) > 48 * 48 * 16) {
                    return Component.translatable("arcane_artifacts.catto.refuse.area_size");
                }
            }
            case CRAFT -> {
                if (CattoPlanner.findRecipe(this, task.item) == null) {
                    return Component.translatable("arcane_artifacts.catto.refuse.no_recipe", task.item.getHoverName(), tier(CattoSkill.CRAFTER));
                }
            }
            default -> {
            }
        }
        return null;
    }

    public void toggleStorage(ServerPlayer player, BlockPos pos, boolean makeFirst) {
        BlockPos p = pos.immutable();
        if (makeFirst) {
            storages.remove(p);
            storages.add(0, p);
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.storage.main", CattoItems.pos(p)));
        } else if (storages.remove(p)) {
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.storage.unbound", CattoItems.pos(p)));
        } else if (storages.size() >= MAX_STORAGES) {
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.storage.full", MAX_STORAGES));
        } else {
            storages.add(p);
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.storage.bound", CattoItems.pos(p), storages.size(), MAX_STORAGES));
        }
        changed();
    }

    /** Menu buttons (sent by vanilla's container button packet). */
    public void button(ServerPlayer player, int id) {
        if (!player.getUUID().equals(getOwnerUUID())) {
            return;
        }
        switch (id) {
            case CattoMenu.BUTTON_PAUSE -> {
                paused = !paused;
                if (!paused) {
                    stay = false;
                    brain().resume();
                }
            }
            case CattoMenu.BUTTON_STOP -> brain().stop();
            case CattoMenu.BUTTON_CALL -> brain().recall(player);
            case CattoMenu.BUTTON_STAY -> {
                stay = !stay;
                if (stay) {
                    getNavigation().stop();
                }
            }
            case CattoMenu.BUTTON_CLEAR -> {
                queue.clear();
                brain().stop();
                paused = false;
            }
            case CattoMenu.BUTTON_GIVE_ALL -> giveAllTo(player);
            default -> {
                int row = id % CattoMenu.ROW_STRIDE;
                int action = id / CattoMenu.ROW_STRIDE;
                if (row < 0 || row >= queue.size()) {
                    return;
                }
                CattoTask task = queue.get(row);
                switch (action) {
                    case CattoMenu.ROW_REMOVE -> {
                        queue.remove(row);
                        if (row == 0) {
                            brain().taskRemoved();
                        }
                    }
                    case CattoMenu.ROW_UP -> {
                        if (row > 0) {
                            queue.remove(row);
                            queue.add(row - 1, task);
                            if (row == 1) {
                                brain().taskRemoved();
                            }
                        }
                    }
                    case CattoMenu.ROW_MORE -> task.count = Math.min(9999, task.count + (task.count < 16 ? 1 : 16));
                    case CattoMenu.ROW_LESS -> task.count = Math.max(task.type == CattoTask.Type.COLLECT ? 0 : 1, task.count - (task.count <= 16 ? 1 : 16));
                    default -> {
                    }
                }
            }
        }
        changed();
    }

    /** Hands the owner everything in the bag (what does not fit falls at their feet). */
    public void giveAllTo(ServerPlayer player) {
        if (distanceToSqr(player) > 16 * 16) {
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.too_far_to_give"));
            return;
        }
        for (int i = 0; i < BAG_SIZE; i++) {
            ItemStack stack = bag.extractItem(i, 64, false);
            if (!stack.isEmpty() && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean owner = isOwnedBy(player);
        if (getOwnerUUID() == null && (held.is(Items.COD) || held.is(Items.SALMON))) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            befriend((ServerPlayer) player, held);
            return InteractionResult.SUCCESS;
        }
        if (!owner) {
            if (held.isEmpty() && player.isShiftKeyDown()) {
                if (level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 2, 0.2, 0.1, 0.2, 0);
                    playSound(SoundEvents.CAT_PURR, 1.0F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level().isClientSide());
            }
            return InteractionResult.PASS;
        }
        if (hand != InteractionHand.MAIN_HAND && held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (held.getItem() instanceof CattoItems.WandItem) {
            CattoItems.WandItem.Mode mode = CattoItems.WandItem.mode(held);
            ItemStack target = player.getOffhandItem();
            if ((mode == CattoItems.WandItem.Mode.FETCH || mode == CattoItems.WandItem.Mode.CRAFT)) {
                if (target.isEmpty()) {
                    CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.wand.need_sample"));
                } else {
                    offer(serverPlayer, mode == CattoItems.WandItem.Mode.FETCH
                            ? CattoTask.fetch(target, target.getCount()) : CattoTask.craft(target, target.getCount()));
                }
                return InteractionResult.SUCCESS;
            }
            openMenu(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof CattoItems.ScrollItem) {
            useScroll(serverPlayer, held);
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof CattoItems.BellItem && player.isShiftKeyDown()) {
            hideIn(serverPlayer, held);
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.COD) || held.is(Items.SALMON)) {
            heal(4.0F);
            if (bond < 100) {
                addBond(nature().fishBond());
            }
            held.consume(1, player);
            ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 3, 0.2, 0.1, 0.2, 0);
            playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (held.is(ItemsRegistry.SOURCE_GEM.get())) {
            if (source >= maxSource()) {
                CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.source_full"));
            } else {
                addSource(100);
                addXp(CattoSkill.ALCHEMIST, 5);
                held.consume(1, player);
                ((ServerLevel) level()).sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.5, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && player.isShiftKeyDown()) {
            pet(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            // Talk first: the conversation has a way to the inventory too.
            CattoDialog.open(serverPlayer, this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * A village Catto and a fish: one chance in three that it chooses the player. A player has one Catto at a time,
     * so a Catto that is alive or asleep in a bell has to be let go first (a fallen one is replaced).
     */
    private void befriend(ServerPlayer player, ItemStack fish) {
        ServerLevel level = (ServerLevel) level();
        CattoSavedData data = CattoSavedData.get(level.getServer());
        CattoSavedData.Entry entry = data.entry(player.getUUID());
        if (find(player) != null || entry != null && entry.status == CattoSavedData.Status.IN_BELL) {
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.village.already"));
            return;
        }
        fish.consume(1, player);
        playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
        if (random.nextInt(3) != 0) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.7, getZ(), 5, 0.2, 0.2, 0.2, 0.01);
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.village.thinking"));
            return;
        }
        tame(player);
        setOrderedToSit(false);
        homePos = null;
        clearRestriction();
        bond = 10;
        data.alive(player.getUUID(), this);
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 7, 0.3, 0.2, 0.3, 0);
        playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable("arcane_artifacts.catto.village.bonded"));
        changed();
    }

    private void pet(ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 4, 0.25, 0.1, 0.25, 0);
        playSound(SoundEvents.CAT_PURR, 1.0F, 1.0F);
        say(Component.translatable("arcane_artifacts.catto.petted_line." + nature().key()), 50);
        if (level.getGameTime() - lastPetTime >= 6000) {
            lastPetTime = level.getGameTime();
            addBond(nature().petBond());
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.petted", bond));
        }
    }

    private void useScroll(ServerPlayer player, ItemStack scroll) {
        ListTag program = CattoItems.ScrollItem.program(scroll);
        if (program.isEmpty()) {
            if (queue.isEmpty()) {
                CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.scroll.nothing"));
                return;
            }
            ListTag list = new ListTag();
            for (CattoTask task : queue) {
                list.add(task.restart(registryAccess()).save(registryAccess()));
            }
            CattoItems.edit(scroll, t -> t.put("Program", list));
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.scroll.written", list.size()));
            return;
        }
        int added = 0;
        for (Tag t : program) {
            CattoTask task = CattoTask.load((CompoundTag) t, registryAccess());
            if (task != null && refusal(task) == null) {
                queue.add(task);
                added++;
            }
        }
        changed();
        CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.scroll.loaded", added, program.size()));
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CattoMenu(id, inventory, this), getDisplayName()),
                buf -> buf.writeVarInt(getId()));
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        long time = level.getGameTime();
        if (speechTicks > 0 && --speechTicks == 0) {
            entityData.set(DATA_SPEECH, Component.empty());
        }
        chatter.tick(level);
        if (entityData.get(DATA_SOURCE) != source) {
            entityData.set(DATA_SOURCE, source);
        }
        if (entityData.get(DATA_MAX_SOURCE) != maxSource()) {
            entityData.set(DATA_MAX_SOURCE, maxSource());
        }
        if (time % 100 == 0 && getOwnerUUID() != null) {
            CattoSavedData.get(level.getServer()).moved(getOwnerUUID(), this);
        }
        // Source: refills from Source Jars around, Source Fur makes a little by itself, modules spend it while working.
        if (time % 40 == 0 && getOwnerUUID() != null && source <= maxSource() - 50
                && com.hollingsworth.arsnouveau.api.util.SourceUtil.takeSource(blockPosition(), level, 6, 50) != null) {
            addSource(50);
            addXp(CattoSkill.ALCHEMIST, 5);
        }
        if (time % 40 == 0 && hasModule(CattoModule.SOURCE_FUR)) {
            addSource(1);
        }
        if (time % 200 == 0 && brain().working()) {
            int upkeep = activeModules() * (tier(CattoSkill.ALCHEMIST) >= 3 ? 1 : 2);
            addSource(-Math.max(0, upkeep - (hasModule(CattoModule.SOURCE_FUR) ? 1 : 0)));
        }
        if (moduleActive(CattoModule.CAT_BOOTS) && isOnFire()) {
            clearFire();
        }
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && attack.getBaseValue() != 3.0 + CattoCombat.fighterDamage(this)) {
            attack.setBaseValue(3.0 + CattoCombat.fighterDamage(this));
        }
        if (time % 100 == 0 && moduleActive(CattoModule.REGROWTH) && getHealth() < getMaxHealth()) {
            heal(1.0F);
        }
        if (fullSet() == CattoGear.Set.DRUID && hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
            removeEffect(net.minecraft.world.effect.MobEffects.POISON);
        }
        // Food in the second paw: eaten when hurt.
        if (time % 40 == 0 && getHealth() < getMaxHealth() * 0.6F && offhand().has(net.minecraft.core.component.DataComponents.FOOD)) {
            var food = offhand().get(net.minecraft.core.component.DataComponents.FOOD);
            heal(Math.max(2, food.nutrition()));
            ItemStack rest = offhand().copy();
            rest.shrink(1);
            equipment.setStackInSlot(SLOT_OFFHAND, rest);
            playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
        }
        // What is shown in the paws: the weapon in a fight, otherwise the tool; the second paw holds its item.
        ItemStack shown = brain().state() == CattoBrain.State.FIGHT && !weapon().isEmpty() ? weapon() : tool();
        // Headwear and armor are worn for real: drawn on Catto, and vanilla armor protects as usual.
        for (var pair : new Object[][]{{net.minecraft.world.entity.EquipmentSlot.HEAD, SLOT_HEAD}, {net.minecraft.world.entity.EquipmentSlot.CHEST, SLOT_ARMOR}}) {
            var slot = (net.minecraft.world.entity.EquipmentSlot) pair[0];
            ItemStack worn = equipment.getStackInSlot((Integer) pair[1]);
            if (!ItemStack.matches(getItemBySlot(slot), worn)) {
                setItemSlot(slot, worn.copy());
                setDropChance(slot, 0.0F);
            }
        }
        if (!ItemStack.matches(getOffhandItem(), offhand())) {
            setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, offhand().copy());
            setDropChance(net.minecraft.world.entity.EquipmentSlot.OFFHAND, 0.0F);
        }
        if (!ItemStack.matches(getMainHandItem(), shown)) {
            setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, shown.copy());
            setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (moduleActive(CattoModule.CAT_BOOTS)) {
            return false;
        }
        return super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (moduleActive(CattoModule.CAT_BOOTS) && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }
        if (!level().isClientSide() && source.getEntity() != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)
                && offhand().getItem() instanceof net.minecraft.world.item.ShieldItem && getRandom().nextFloat() < 0.4F) {
            ItemStack shield = offhand().copy();
            shield.hurtAndBreak(1, this, net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            equipment.setStackInSlot(SLOT_OFFHAND, shield);
            playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
            return false;
        }
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            amount *= 1.0F - Math.min(0.6F, armor() * 0.04F);
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide()) {
            brain().hurt();
        }
        return hurt;
    }

    @Override
    public void die(DamageSource cause) {
        if (level() instanceof ServerLevel level && getOwnerUUID() != null) {
            UUID owner = getOwnerUUID();
            CompoundTag snapshot = saveWithoutId(new CompoundTag());
            snapshot.putFloat("Health", getMaxHealth());
            snapshot.remove("Fire");
            snapshot.remove("active_effects");
            CattoSavedData.get(level.getServer()).fallen(owner, snapshot);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("arcane_artifacts.catto.fallen", getDisplayName()));
            }
        }
        super.die(cause);
    }

    // Catto never breeds and never despawns.
    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public Cat getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag catto = new CompoundTag();
        catto.put("Equipment", equipment.serializeNBT(registryAccess()));
        catto.put("Bag", bag.serializeNBT(registryAccess()));
        ListTag tasks = new ListTag();
        queue.forEach(task -> tasks.add(task.save(registryAccess())));
        catto.put("Queue", tasks);
        catto.putLongArray("Storages", storages.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag log = new ListTag();
        reports.forEach(r -> log.add(r.save()));
        catto.put("Reports", log);
        catto.putIntArray("Xp", xp);
        catto.putBoolean("Paused", paused);
        catto.putBoolean("Stay", stay);
        catto.putInt("Bond", bond);
        catto.putInt("Source", source);
        catto.putString("Nature", nature().name());
        catto.putString("Coat", coat().name());
        catto.putString("Mode", mode.name());
        if (guardPos != null) {
            catto.putLong("GuardPos", guardPos.asLong());
        }
        if (homePos != null) {
            catto.putLong("HomePos", homePos.asLong());
        }
        catto.putLong("LastPet", lastPetTime);
        tag.put("Catto", catto);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("Catto")) {
            // A cat turned into Catto by the ritual keeps (roughly) its coat.
            if (tag.contains("variant")) {
                setCoat(CattoCoat.fromVanilla(tag.getString("variant")));
            }
            return;
        }
        CompoundTag catto = tag.getCompound("Catto");
        equipment.deserializeNBT(registryAccess(), catto.getCompound("Equipment"));
        if (equipment.getSlots() != EQUIPMENT_SIZE) {
            // Saved before the six-slot equipment: start the equipment afresh.
            equipment.setSize(EQUIPMENT_SIZE);
        }
        bag.deserializeNBT(registryAccess(), catto.getCompound("Bag"));
        queue.clear();
        for (Tag t : catto.getList("Queue", Tag.TAG_COMPOUND)) {
            CattoTask task = CattoTask.load((CompoundTag) t, registryAccess());
            if (task != null) {
                queue.add(task);
            }
        }
        storages.clear();
        for (long l : catto.getLongArray("Storages")) {
            storages.add(BlockPos.of(l));
        }
        reports.clear();
        for (Tag t : catto.getList("Reports", Tag.TAG_COMPOUND)) {
            reports.addLast(Report.load((CompoundTag) t));
        }
        int[] saved = catto.getIntArray("Xp");
        System.arraycopy(saved, 0, xp, 0, Math.min(saved.length, xp.length));
        paused = catto.getBoolean("Paused");
        stay = catto.getBoolean("Stay");
        bond = catto.getInt("Bond");
        source = catto.getInt("Source");
        try {
            setCoat(CattoCoat.valueOf(catto.getString("Coat")));
        } catch (IllegalArgumentException ignored) {
            // keep the rolled coat
        }
        try {
            mode = CattoMode.valueOf(catto.getString("Mode"));
        } catch (IllegalArgumentException ignored) {
            mode = CattoMode.NONE;
        }
        guardPos = catto.contains("GuardPos") ? BlockPos.of(catto.getLong("GuardPos")) : null;
        homePos = catto.contains("HomePos") ? BlockPos.of(catto.getLong("HomePos")) : null;
        if (homePos != null) {
            restrictTo(homePos, 10);
        }
        try {
            nature = CattoNature.valueOf(catto.getString("Nature"));
        } catch (IllegalArgumentException ignored) {
            // older Catto: keep the one rolled at creation
        }
        lastPetTime = catto.getLong("LastPet");
        entityData.set(DATA_SOURCE, source);
    }

    /** Everything the owner's menu shows that is not in slots. */
    public CompoundTag menuData() {
        CompoundTag tag = new CompoundTag();
        ListTag tasks = new ListTag();
        queue.forEach(task -> tasks.add(task.save(registryAccess())));
        tag.put("Queue", tasks);
        ListTag log = new ListTag();
        reports.forEach(r -> log.add(r.save()));
        tag.put("Reports", log);
        tag.putIntArray("Xp", xp);
        tag.putBoolean("Paused", paused);
        tag.putBoolean("Stay", stay);
        tag.putInt("Bond", bond);
        tag.putInt("Source", source);
        tag.putInt("MaxSource", maxSource());
        tag.putInt("QueueCap", queueCap());
        tag.putInt("Capacity", capacity());
        tag.putInt("Storages", storages.size());
        tag.putString("Nature", nature().name());
        tag.putString("Mode", mode.name());
        tag.putInt("Armor", armor() + getArmorValue());
        CattoGear.Set set = fullSet();
        if (set != null) {
            tag.putString("Set", set.name());
        }
        return tag;
    }
}
