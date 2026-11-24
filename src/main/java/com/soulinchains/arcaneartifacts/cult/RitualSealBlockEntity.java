package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Ritual Seal: the centre and hearth of a Cultist circle, and the rite itself (a server-side state machine).
 * <ul>
 *     <li><b>Pairing</b>: a player not yet paired lights the seal with the Ritual Knife; within {@link #PAIRING_WAIT} another
 *     player confirms at an anchor with the Oath Shield; both stay in the circle for {@link #PAIRING_TICKS}. The first is
 *     the Confessor, the second the Warden.</li>
 *     <li><b>Rite of the Ash Witness</b>: the Confessor lays 4 ash, 2 bone shards and a red candle on the seal; the Warden
 *     lights at least two anchors; the Confessor starts the channel with the knife. During the channel the seal flares
 *     ("inscription windows"), the Confessor strikes it with the knife in the window, the Warden holds the outer ring and
 *     mends the anchors, Torn Acolytes come in two waves. The quality (0-100%) decides what rises.</li>
 * </ul>
 * Alone (no Warden among the anchor lighters) the quality is capped at {@link Config#CULT_SOLO_CAP}; a player in creative may
 * hold a full rite alone when {@link Config#CULT_CREATIVE_SOLO} is on.
 */
public class RitualSealBlockEntity extends BlockEntity {
    public enum Phase { IDLE, PAIRING_WAIT, PAIRING, CHANNEL }

    public static final int RADIUS = 6;
    public static final int INNER = 3;
    public static final int PAIRING_WAIT = 300;
    public static final int PAIRING_TICKS = 200;
    public static final int ASH_TICKS = 500;
    public static final int WINDOW_EVERY = 80;
    public static final int WINDOW_TICKS = 20;
    public static final int FINAL_WINDOW = 40;
    public static final int ASH_NEEDED = 4;
    public static final int SHARDS_NEEDED = 2;
    public static final int CANDLES_NEEDED = 1;
    public static final int ANCHORS_NEEDED = 2;
    public static final float START_QUALITY = 50F;
    public static final int RANK_TWO_STEADY = 3;
    private static final Map<UUID, RitualSealBlockEntity> BY_CONFESSOR = new HashMap<>();
    private static final Map<UUID, RitualSealBlockEntity> BY_WARDEN = new HashMap<>();
    private float voiceGiven;

    private Phase phase = Phase.IDLE;
    private int timer;
    private float quality;
    private int ash;
    private int shards;
    private int candles;
    @Nullable
    private Player confessor;
    @Nullable
    private Player warden;
    private boolean solo;
    private int windowOpenUntil = -1;
    private boolean windowUsed;
    private int nextWindow;
    private int wardenOut;
    private int partnerFar;
    private int confessorOut;
    private boolean wave1;
    private boolean wave2;
    private boolean closed;
    private boolean wardenSealed;
    private int perfectStrikes;
    private final List<BlockPos> anchors = new ArrayList<>();
    /** The rite drawn on the seal (Book of Formulas). */
    private RiteScheme scheme = RiteScheme.ASH_WITNESS;
    private KnifeInlay inlay = KnifeInlay.NONE;
    private boolean censer;
    private int duration = ASH_TICKS;
    private int windowEvery = WINDOW_EVERY;
    public static final int CENSER_RADIUS = 3;
    public static final int CENSER_WINDOW_BONUS = 6;

    public RitualSealBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RITUAL_SEAL_ENTITY.get(), pos, state);
    }

    // ------------------------------------------------------------------ lookups

    /** The seal whose circle holds this position, if any. */
    @Nullable
    public static RitualSealBlockEntity near(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -2, -RADIUS), pos.offset(RADIUS, 2, RADIUS))) {
            if (level.getBlockEntity(p) instanceof RitualSealBlockEntity seal) {
                return seal;
            }
        }
        return null;
    }

    @Nullable
    public static RitualSealBlockEntity ofConfessor(Player player) {
        RitualSealBlockEntity seal = BY_CONFESSOR.get(player.getUUID());
        if (seal != null && (seal.isRemoved() || seal.phase != Phase.CHANNEL)) {
            BY_CONFESSOR.remove(player.getUUID());
            return null;
        }
        return seal;
    }

    /** The rite this player is the Warden of, if one runs. */
    @Nullable
    public static RitualSealBlockEntity ofWarden(Player player) {
        RitualSealBlockEntity seal = BY_WARDEN.get(player.getUUID());
        if (seal != null && (seal.isRemoved() || seal.phase != Phase.CHANNEL)) {
            BY_WARDEN.remove(player.getUUID());
            return null;
        }
        return seal;
    }

    /** Takes the missing reagents from the player's inventory. */
    public void pullReagents(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && !reagentsReady(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            while (!stack.isEmpty() && isReagent(stack) && offer(stack)) {
                stack.shrink(1);
            }
        }
    }

    public List<RitualAnchorBlockEntity> anchorsAround() {
        List<RitualAnchorBlockEntity> found = new ArrayList<>();
        if (level == null) {
            return found;
        }
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RADIUS, -2, -RADIUS), worldPosition.offset(RADIUS, 2, RADIUS))) {
            if (level.getBlockEntity(p) instanceof RitualAnchorBlockEntity anchor) {
                found.add(anchor);
                if (found.size() >= 4) {
                    break;
                }
            }
        }
        return found;
    }

    public Phase phase() {
        return phase;
    }

    public float quality() {
        return quality;
    }

    public boolean windowOpen() {
        return phase == Phase.CHANNEL && timer <= windowOpenUntil && !windowUsed;
    }

    public boolean finalWindow() {
        return phase == Phase.CHANNEL && timer >= duration - FINAL_WINDOW;
    }

    public int duration() {
        return duration;
    }

    public RiteScheme scheme() {
        return scheme;
    }

    /** The Book of Formulas: the next rite this seal knows. */
    public RiteScheme cycleScheme() {
        if (phase != Phase.CHANNEL) {
            scheme = scheme.next();
            setChanged();
        }
        return scheme;
    }

    /** A Ritual Censer within 3 blocks of the seal. */
    public boolean censerNear() {
        if (level == null) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-CENSER_RADIUS, -1, -CENSER_RADIUS), worldPosition.offset(CENSER_RADIUS, 1, CENSER_RADIUS))) {
            if (level.getBlockState(p).is(ModBlocks.RITUAL_CENSER.get())) {
                return true;
            }
        }
        return false;
    }

    /** How hard a Torn Acolyte's blow lands on an anchor of this circle (the bone inlay softens it). */
    public int anchorDamage(int base) {
        return inlay == KnifeInlay.BONE ? Math.round(base * 0.75F) : base;
    }

    /** ... and the Seal-Keeper's Helm on a Warden standing near that anchor. */
    public int anchorDamage(int base, BlockPos anchor) {
        return CultGear.anchorDamage(anchorDamage(base), solo ? null : warden, Vec3.atCenterOf(anchor));
    }

    /** The Seal-Wright's Hammer on an anchor during the rite: a bigger mend. */
    public boolean onHammerAtAnchor(Player player, RitualAnchorBlockEntity anchor) {
        if (phase != Phase.CHANNEL) {
            say(player, "hammer_rite_only");
            return false;
        }
        int gained = anchor.repair(CultGear.hammerRepair(player));
        if (gained > 0) {
            burst(anchor.getBlockPos(), 0xD6B2A5);
            sound(anchor.getBlockPos(), SoundEvents.ANVIL_PLACE, 0.5F, 1.6F);
        }
        return gained > 0;
    }

    public boolean solo() {
        return solo;
    }

    public int timer() {
        return timer;
    }

    private double distance(Player player) {
        return Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(worldPosition)));
    }

    // ------------------------------------------------------------------ reagents

    public static boolean isReagent(ItemStack stack) {
        return stack.is(ModItems.RITUAL_ASH.get()) || stack.is(ModItems.BONE_SHARD.get()) || stack.is(Items.RED_CANDLE);
    }

    /** Lays one reagent on the seal; returns false when no more of it is needed. */
    public boolean offer(ItemStack stack) {
        if (phase == Phase.CHANNEL) {
            return false;
        }
        Item item = stack.getItem();
        if (item == ModItems.RITUAL_ASH.get() && ash < ASH_NEEDED) {
            ash++;
        } else if (item == ModItems.BONE_SHARD.get() && shards < SHARDS_NEEDED) {
            shards++;
        } else if (item == Items.RED_CANDLE && candles < CANDLES_NEEDED) {
            candles++;
        } else {
            return false;
        }
        setChanged();
        return true;
    }

    public boolean reagentsReady() {
        return ash >= ASH_NEEDED && shards >= SHARDS_NEEDED && candles >= CANDLES_NEEDED;
    }

    public Component reagentsLine() {
        return Component.translatable("arcane_artifacts.cult.reagents", ash, ASH_NEEDED, shards, SHARDS_NEEDED, candles, CANDLES_NEEDED);
    }

    // ------------------------------------------------------------------ knife & shield

    /** The Ritual Knife on the seal. */
    public void onKnife(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }
        CultData data = Cult.get(player);
        switch (phase) {
            case CHANNEL -> {
                if (player == confessor || player.getUUID().equals(confessor == null ? null : confessor.getUUID())) {
                    strike(player);
                }
            }
            case PAIRING_WAIT, PAIRING -> say(player, "pairing_busy");
            case IDLE -> {
                if (player.isShiftKeyDown() && data.paired()) {
                    Cult.unpair(player, null);
                } else if (!data.paired()) {
                    beginPairing(player);
                } else if (data.role() != CultRole.CONFESSOR) {
                    say(player, "knife_confessor_only");
                } else {
                    tryStart(player);
                }
            }
        }
    }

    /** The Oath Shield at one of the anchors of this circle. */
    public void onShieldAtAnchor(Player player, RitualAnchorBlockEntity anchor) {
        switch (phase) {
            case PAIRING_WAIT -> confirmPairing(player, anchor);
            case PAIRING -> say(player, "pairing_busy");
            case CHANNEL -> {
                if (finalWindow() && player == warden && !wardenSealed) {
                    wardenSealed = true;
                    quality += 3F;
                    burst(anchor.getBlockPos(), 0xB9463D);
                }
                int gained = anchor.repair(CultGear.repairAmount(player) + CultGearEvents.useAnchorCharge(player));
                if (gained > 0) {
                    sound(anchor.getBlockPos(), SoundEvents.ANVIL_USE, 0.6F, 1.4F);
                }
            }
            case IDLE -> {
                anchor.activate(player, CultGear.startStability(player));
                burst(anchor.getBlockPos(), 0xB9463D);
                sound(anchor.getBlockPos(), SoundEvents.FLINTANDSTEEL_USE, 1.0F, 0.8F);
                say(player, "anchor_lit");
            }
        }
    }

    // ------------------------------------------------------------------ pairing

    public void beginPairing(Player player) {
        if (anchorsAround().isEmpty()) {
            say(player, "need_anchor");
            return;
        }
        phase = Phase.PAIRING_WAIT;
        timer = 0;
        confessor = player;
        warden = null;
        say(player, "pairing_wait");
        sound(worldPosition, SoundEvents.BELL_RESONATE, 1.0F, 0.6F);
    }

    public void confirmPairing(Player player, @Nullable RitualAnchorBlockEntity anchor) {
        if (confessor == null || player == confessor || player.getUUID().equals(confessor.getUUID())) {
            say(player, "pairing_other");
            return;
        }
        if (Cult.get(player).paired()) {
            say(player, "already_paired");
            return;
        }
        warden = player;
        phase = Phase.PAIRING;
        timer = 0;
        say(confessor, "pairing_started");
        say(player, "pairing_started");
        if (anchor != null) {
            burst(anchor.getBlockPos(), 0xB9463D);
        }
    }

    // ------------------------------------------------------------------ the rite

    public void tryStart(Player player) {
        if (!reagentsReady() && CultGear.keepsDebt(player)) {
            // the Book of Arrears: the seal takes what is missing from the Confessor's pack
            pullReagents(player);
        }
        if (!reagentsReady()) {
            player.displayClientMessage(reagentsLine(), true);
            return;
        }
        List<RitualAnchorBlockEntity> lit = anchorsAround().stream().filter(RitualAnchorBlockEntity::active).toList();
        if (lit.size() < ANCHORS_NEEDED) {
            say(player, "need_anchors", ANCHORS_NEEDED);
            return;
        }
        UUID partner = Cult.get(player).partner().orElse(null);
        warden = null;
        for (RitualAnchorBlockEntity anchor : lit) {
            if (partner != null && partner.equals(anchor.activator()) && anchor.activatorRef() != null) {
                warden = anchor.activatorRef();
            }
        }
        if (warden == null && partner != null && level != null) {
            warden = level.getPlayerByUUID(partner);
            if (warden != null && lit.stream().noneMatch(a -> partner.equals(a.activator()))) {
                warden = null;
            }
        }
        boolean creativeAlone = player.isCreative() && Config.CULT_CREATIVE_SOLO.get();
        solo = (warden == null || Cult.role(warden) != CultRole.WARDEN) && !creativeAlone;
        confessor = player;
        ash -= ASH_NEEDED;
        shards -= SHARDS_NEEDED;
        candles -= CANDLES_NEEDED;
        anchors.clear();
        for (RitualAnchorBlockEntity anchor : lit) {
            anchors.add(anchor.getBlockPos());
        }
        phase = Phase.CHANNEL;
        timer = 0;
        quality = START_QUALITY + Math.min(10, Cult.get(player).debt()) + CultGear.pairBonus(confessor, warden);
        inlay = KnifeInlay.of(player.getMainHandItem());
        censer = censerNear();
        duration = inlay == KnifeInlay.IRON ? Math.round(ASH_TICKS * 0.8F) : ASH_TICKS;
        windowEvery = inlay == KnifeInlay.IRON ? Math.round(WINDOW_EVERY * 0.8F) : WINDOW_EVERY;
        nextWindow = windowEvery / 2;
        windowOpenUntil = -1;
        windowUsed = false;
        wardenOut = partnerFar = confessorOut = 0;
        wave1 = wave2 = closed = wardenSealed = false;
        perfectStrikes = 0;
        BY_CONFESSOR.put(player.getUUID(), this);
        if (warden != null && !solo) {
            BY_WARDEN.put(warden.getUUID(), this);
        }
        voiceGiven = 0F;
        say(player, solo ? "rite_started_solo" : "rite_started");
        if (warden != null && !solo) {
            say(warden, "rite_started");
        }
        sound(worldPosition, SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 0.6F);
        setChanged();
    }

    /** The knife strikes the seal during the channel. */
    public void strike(Player player) {
        if (finalWindow()) {
            if (!closed) {
                closed = true;
                quality += CultGear.strikeBonus(player, true);
                burst(worldPosition, 0xFAF7F3);
                sound(worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.7F);
                finish();
            }
            return;
        }
        if (windowOpen()) {
            windowUsed = true;
            perfectStrikes++;
            quality += CultGear.strikeBonus(player, false);
            float voice = CultGear.voiceBonus(player, voiceGiven);
            voiceGiven += voice;
            quality += voice;
            burst(worldPosition, 0xB9463D);
            sound(worldPosition, SoundEvents.AMETHYST_BLOCK_HIT, 1.2F, 1.2F);
        } else {
            quality -= CultGear.missPenalty(player);
            sound(worldPosition, SoundEvents.CHAIN_HIT, 0.8F, 0.6F);
        }
    }

    /** The Confessor was hurt while reading. */
    public void onConfessorHurt() {
        quality -= CultGear.hurtPenalty(confessor);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RitualSealBlockEntity seal) {
        seal.tick();
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        switch (phase) {
            case IDLE -> {
            }
            case PAIRING_WAIT -> {
                if (++timer > PAIRING_WAIT || confessor == null || !confessor.isAlive()) {
                    if (confessor != null) {
                        say(confessor, "pairing_timeout");
                    }
                    reset();
                }
            }
            case PAIRING -> {
                timer++;
                if (confessor == null || warden == null || !confessor.isAlive() || !warden.isAlive()
                        || distance(confessor) > RADIUS + 1 || distance(warden) > RADIUS + 1) {
                    if (confessor != null) {
                        say(confessor, "pairing_broken");
                    }
                    if (warden != null) {
                        say(warden, "pairing_broken");
                    }
                    reset();
                    return;
                }
                if (timer % 10 == 0) {
                    ring(server, 0xB9463D);
                }
                if (timer >= PAIRING_TICKS) {
                    Cult.pair(confessor, warden);
                    sound(worldPosition, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.6F);
                    reset();
                }
            }
            case CHANNEL -> channel(server);
        }
    }

    private void channel(ServerLevel server) {
        timer++;
        if (confessor == null || !confessor.isAlive() || confessor.isRemoved() && !(confessor instanceof net.neoforged.neoforge.common.util.FakePlayer)
                || distance(confessor) > 32) {
            fail("broken_confessor");
            return;
        }
        // inscription windows
        if (timer >= nextWindow && timer < duration - FINAL_WINDOW) {
            if (windowOpenUntil >= 0 && !windowUsed) {
                quality -= 2F;
            }
            windowOpenUntil = timer + CultGear.windowTicks(confessor) + (censer ? CENSER_WINDOW_BONUS : 0);
            windowUsed = false;
            nextWindow = timer + windowEvery;
            sound(worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.6F);
        }
        if (windowOpen()) {
            server.sendParticles(new DustParticleOptions(new Vector3f(0.72F, 0.27F, 0.24F), 1.6F), worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 4, 0.25, 0.1, 0.25, 0);
        }
        if (timer % 20 == 0) {
            // the Confessor reads from the inner zone; the Warden holds the outer ring; the pair stays bound
            if (distance(confessor) > INNER + 1) {
                quality -= 1F;
            }
            if (warden != null && !solo) {
                double d = distance(warden);
                wardenOut = d > RADIUS + 2 ? wardenOut + 1 : 0;
                if (wardenOut > 3) {
                    quality -= 1F;
                }
                partnerFar = warden.distanceTo(confessor) > Config.CULT_LINK_RANGE.get() || !warden.isAlive() ? partnerFar + 1 : 0;
                if (partnerFar > 5) {
                    quality -= 2F;
                }
            }
            ring(server, 0x6F2927);
        }
        // anchors: the Warden standing by one keeps it steady
        if (timer % 10 == 0) {
            for (BlockPos pos : anchors) {
                if (server.getBlockEntity(pos) instanceof RitualAnchorBlockEntity anchor && anchor.active()
                        && warden != null && !solo && warden.distanceToSqr(Vec3.atCenterOf(pos)) < 6.25) {
                    anchor.repair(2);
                }
            }
        }
        // waves of Torn Acolytes
        if (!wave1 && timer >= duration / 4) {
            wave1 = true;
            wave(server, censer ? 1 : 2);
        }
        if (!wave2 && timer >= duration * 3 / 5) {
            wave2 = true;
            wave(server, censer ? 2 : 3);
        }
        if (timer == duration - FINAL_WINDOW) {
            sound(worldPosition, SoundEvents.BELL_BLOCK, 1.2F, 0.5F);
            say(confessor, "close_now");
            if (warden != null && !solo) {
                say(warden, "seal_now");
            }
        }
        quality = Mth.clamp(quality, 0F, 100F);
        if (timer % 5 == 0) {
            sync();
        }
        if (timer >= duration) {
            quality -= 10F;
            finish();
        }
    }

    private void wave(ServerLevel server, int count) {
        for (int i = 0; i < count; i++) {
            TornAcolyte acolyte = ModEntities.TORN_ACOLYTE.get().create(server);
            if (acolyte == null) {
                continue;
            }
            double angle = server.random.nextDouble() * Math.PI * 2;
            BlockPos at = BlockPos.containing(worldPosition.getX() + 0.5 + Math.cos(angle) * (RADIUS + 3), worldPosition.getY(),
                    worldPosition.getZ() + 0.5 + Math.sin(angle) * (RADIUS + 3));
            at = server.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
            if (Math.abs(at.getY() - worldPosition.getY()) > 4) {
                at = new BlockPos(at.getX(), worldPosition.getY(), at.getZ());
            }
            acolyte.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, server.random.nextFloat() * 360, 0);
            acolyte.besiege(worldPosition);
            server.addFreshEntity(acolyte);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, acolyte.getX(), acolyte.getY() + 1, acolyte.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        sound(worldPosition, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.0F, 0.5F);
    }

    /** The rite ends: the quality decides what rises. */
    private void finish() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int broken = 0;
        for (BlockPos pos : anchors) {
            if (server.getBlockEntity(pos) instanceof RitualAnchorBlockEntity anchor && anchor.broken()) {
                broken++;
            }
        }
        quality -= broken * 5F;
        if (solo) {
            quality = Math.min(quality, Config.CULT_SOLO_CAP.get().floatValue() * 100F);
        }
        quality = Mth.clamp(quality, 0F, 100F);
        RitualQuality tier = RitualQuality.of(quality);
        int risen = RitualSummons.summonAshWitness(server, worldPosition.above(), tier, confessor, warden, inlay);
        int dust = tier.ordinal() >= RitualQuality.STEADY.ordinal() ? tier.ordinal() - 1 : 0;
        if (dust > 0) {
            net.minecraft.world.Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                    new ItemStack(ModItems.VOW_DUST.get(), dust));
        }
        Component result = Component.translatable("arcane_artifacts.cult.rite_done", Math.round(quality), tier.displayName(), risen);
        if (confessor != null) {
            confessor.displayClientMessage(result, false);
            record(confessor, tier);
        }
        if (warden != null && !solo) {
            warden.displayClientMessage(result, false);
            record(warden, tier);
        }
        sync();
        reset();
    }

    private void record(Player player, RitualQuality tier) {
        CultData data = Cult.get(player);
        if (tier.ordinal() >= RitualQuality.STEADY.ordinal()) {
            CultData next = data.withSteady(data.steady() + 1).withDebt(0);
            if (next.steady() >= RANK_TWO_STEADY && next.rank() < 2) {
                next = new CultData(next.role(), next.partner(), 2, next.steady(), next.debt());
                player.displayClientMessage(Component.translatable("arcane_artifacts.cult.rank_up", Component.translatable("arcane_artifacts.cult.rank.2")), false);
            }
            Cult.set(player, next);
        } else if (tier == RitualQuality.BROKEN) {
            Cult.set(player, data.withDebt(Math.min(10, data.debt() + (CultGear.keepsDebt(player) ? 1 : 0))));
        }
    }

    private void fail(String reason) {
        if (confessor != null) {
            say(confessor, reason);
        }
        if (warden != null && !solo) {
            say(warden, reason);
        }
        sound(worldPosition, SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.6F);
        quality = 0;
        sync();
        reset();
    }

    private void reset() {
        if (confessor != null) {
            BY_CONFESSOR.remove(confessor.getUUID());
        }
        if (warden != null) {
            BY_WARDEN.remove(warden.getUUID());
        }
        if (level != null) {
            for (BlockPos pos : anchors) {
                if (level.getBlockEntity(pos) instanceof RitualAnchorBlockEntity anchor) {
                    anchor.reset();
                }
            }
        }
        anchors.clear();
        phase = Phase.IDLE;
        timer = 0;
        confessor = null;
        warden = null;
        windowOpenUntil = -1;
        setChanged();
    }

    @Override
    public void setRemoved() {
        if (confessor != null) {
            BY_CONFESSOR.remove(confessor.getUUID());
        }
        super.setRemoved();
    }

    // ------------------------------------------------------------------ sync & effects

    private void sync() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int[] stabilities = new int[anchors.size()];
        for (int i = 0; i < anchors.size(); i++) {
            stabilities[i] = server.getBlockEntity(anchors.get(i)) instanceof RitualAnchorBlockEntity a ? (a.broken() ? -1 : a.stability()) : -1;
        }
        CultNetwork.RitualPayload payload = new CultNetwork.RitualPayload(phase == Phase.CHANNEL, Math.round(quality),
                Math.max(0, duration - timer), windowOpen() || finalWindow() && !closed, finalWindow(), solo, stabilities);
        for (Player player : new Player[]{confessor, warden}) {
            if (player instanceof ServerPlayer sp && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer) && sp.connection != null) {
                PacketDistributor.sendToPlayer(sp, payload);
            }
        }
    }

    private void ring(ServerLevel server, int rgb) {
        Vector3f color = new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            server.sendParticles(new DustParticleOptions(color, 1.0F), worldPosition.getX() + 0.5 + Math.cos(a) * RADIUS, worldPosition.getY() + 0.2,
                    worldPosition.getZ() + 0.5 + Math.sin(a) * RADIUS, 1, 0, 0, 0, 0);
        }
    }

    private void burst(BlockPos pos, int rgb) {
        if (level instanceof ServerLevel server) {
            Vector3f color = new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F);
            server.sendParticles(new DustParticleOptions(color, 1.5F), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 16, 0.25, 0.25, 0.25, 0);
        }
    }

    private void sound(BlockPos pos, net.minecraft.sounds.SoundEvent event, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, pos, event, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private static void say(@Nullable Player player, String key, Object... args) {
        if (player != null) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.cult." + key, args), true);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Ash", ash);
        tag.putInt("Shards", shards);
        tag.putInt("Candles", candles);
        tag.putString("Scheme", scheme.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ash = tag.getInt("Ash");
        shards = tag.getInt("Shards");
        candles = tag.getInt("Candles");
        scheme = RiteScheme.byName(tag.getString("Scheme"));
    }

}
