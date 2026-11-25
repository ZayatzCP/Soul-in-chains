package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Heart of the Circle: the server side of a chalk rite.
 * <ol>
 *     <li><b>Start</b> - the Book of Sigils on the Heart: the site is scanned, the four rite places name a rite, enough players
 *     stand on the field, the offering lies on the pedestal (and a cage stands on the field if the rite wants one). The
 *     starting quality comes from candles, lanterns, the censer, sigils and modifiers; roles are handed out.</li>
 *     <li><b>Channel</b> - the Reader reads, sigils fade one after another and the Tracer must retrace them, wind and Echoes put
 *     out candles, the Heart asks for further offerings and for the caged creature, Echoes come over the boundary in waves.</li>
 *     <li><b>Closing</b> - the Reader closes the rite with the Book on the Heart within 10 s; late, the summoned come hostile.</li>
 * </ol>
 */
public class RiteHeartBlockEntity extends BlockEntity {
    public enum Phase { IDLE, CHANNEL, CLOSING }

    public static final int FADE_EVERY = 120;
    public static final int FADE_WINDOW = 60;
    public static final int WIND_EVERY = 160;
    public static final int OFFER_WINDOW = 200;
    public static final int CLOSE_TICKS = 200;
    public static final float TRACE_GAIN = 2F;
    public static final float TRACE_MISS = 5F;
    public static final float OFFER_GAIN = 3F;
    public static final float OFFER_MISS = 10F;
    public static final float CAGE_MISS = 15F;
    private static final Set<RiteHeartBlockEntity> ACTIVE = ConcurrentHashMap.newKeySet();

    private Phase phase = Phase.IDLE;
    @Nullable
    private RiteDefinition rite;
    @Nullable
    private RiteSite site;
    private RiteModifiers mods = new RiteModifiers();
    private final List<Player> participants = new ArrayList<>();
    private final Map<UUID, EnumSet<RiteRole>> roles = new HashMap<>();
    private final List<BlockPos> candles = new ArrayList<>();
    private float quality;
    private int timer;
    private int duration;
    private int readProgress;
    @Nullable
    private RiteSite.Place fading;
    private int fadingUntil;
    private int nextFade;
    private int mainMisses;
    private int offerIndex;
    @Nullable
    private RiteDefinition.Offering awaiting;
    private int awaitingUntil = -1;
    private int cageUntil = -1;
    private boolean cageAsked;
    @Nullable
    private BlockPos cagePos;
    @Nullable
    private CompoundTag cageCreature;
    private boolean cageOtherworldly;
    private int waves;
    private int closeUntil;
    private int taint;
    private int lastHalf = 4;

    public RiteHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RITE_HEART_ENTITY.get(), pos, state);
    }

    public Phase phase() {
        return phase;
    }

    public float quality() {
        return quality;
    }

    public int taint() {
        return taint;
    }

    public void setTaint(int value) {
        taint = Math.max(0, value);
        setChanged();
    }

    @Nullable
    public RiteDefinition rite() {
        return rite;
    }

    @Nullable
    public RiteSite.Place fading() {
        return fading;
    }

    public List<Player> participants() {
        return participants;
    }

    public boolean has(Player player, RiteRole role) {
        EnumSet<RiteRole> set = roles.get(player.getUUID());
        return set != null && set.contains(role);
    }

    @Nullable
    public Player holder(RiteRole role) {
        for (Player p : participants) {
            if (has(p, role)) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ lookups from other blocks and items

    @Nullable
    static RiteHeartBlockEntity covering(Level level, BlockPos pos) {
        for (RiteHeartBlockEntity heart : ACTIVE) {
            if (heart.isRemoved() || heart.level != level || heart.phase == Phase.IDLE) {
                ACTIVE.remove(heart);
                continue;
            }
            if (heart.site != null && heart.site.inside(pos) && Math.abs(pos.getY() - heart.worldPosition.getY()) <= 6) {
                return heart;
            }
        }
        return null;
    }

    @Nullable
    static RiteHeartBlockEntity of(Level level, BlockPos heartPos) {
        return level.getBlockEntity(heartPos) instanceof RiteHeartBlockEntity heart ? heart : null;
    }

    public static boolean running(Level level, BlockPos heartPos) {
        RiteHeartBlockEntity heart = of(level, heartPos);
        return heart != null && heart.phase != Phase.IDLE;
    }

    /** A participant who has work to do in a running rite (Echoes go for them first). */
    public static boolean busy(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        for (RiteHeartBlockEntity heart : ACTIVE) {
            if (heart.phase != Phase.IDLE && heart.participants.contains(player)) {
                EnumSet<RiteRole> set = heart.roles.get(player.getUUID());
                return set != null && !(set.size() == 1 && set.contains(RiteRole.GUARD));
            }
        }
        return false;
    }

    /** The reader of a running rite was hit: the line breaks off. */
    public static void readerHurt(Player player) {
        for (RiteHeartBlockEntity heart : ACTIVE) {
            if (heart.phase == Phase.CHANNEL && heart.has(player, RiteRole.READER)) {
                heart.readProgress = Math.max(0, heart.readProgress - 60);
            }
        }
    }

    /** Chalk touched a mark during a rite: if it belongs to the fading sigil and the Tracer did it, the sigil is retraced. */
    public static boolean traceAt(Level level, BlockPos mark, Player player, ChalkColor color) {
        RiteHeartBlockEntity heart = covering(level, mark);
        if (heart == null || heart.phase != Phase.CHANNEL || heart.fading == null || heart.site == null
                || !heart.fading.equals(heart.site.placeOf(mark))) {
            return false;
        }
        if (!heart.has(player, RiteRole.TRACER)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.not_your_task", RiteRole.TRACER.displayName()), true);
            return true;
        }
        SigilReading reading = heart.site.at(heart.fading);
        if (reading.sigil() == null || !color.serves(reading.sigil().color)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.chalk_other_color"), true);
            return true;
        }
        heart.quality = Math.min(100F, heart.quality + TRACE_GAIN);
        heart.burst(heart.site.corner(heart.fading).offset(1, 0, 1), 0xF1EEE6);
        heart.fading = null;
        level.playSound(null, mark, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.3F);
        return true;
    }

    /** Whether the pedestal on this Heart takes {@code stack} from {@code player} now. */
    public static boolean acceptsOffering(Level level, BlockPos heartPos, ItemStack stack, Player player) {
        RiteHeartBlockEntity heart = of(level, heartPos);
        if (heart == null || heart.phase == Phase.IDLE) {
            return true;
        }
        if (heart.awaiting == null || !heart.awaiting.matches(stack)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.offering_refused"), true);
            return false;
        }
        if (!heart.has(player, RiteRole.SACRIFICER)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.not_your_task", RiteRole.SACRIFICER.displayName()), true);
            return false;
        }
        return true;
    }

    /** An offering was laid on the pedestal during the rite: the Heart burns it. */
    public static void offered(Level level, BlockPos heartPos, Player player) {
        RiteHeartBlockEntity heart = of(level, heartPos);
        if (heart == null || heart.phase == Phase.IDLE || heart.awaiting == null) {
            return;
        }
        heart.pedestal().ifPresent(p -> p.burn(heart.awaiting.count()));
        heart.quality = Math.min(100F, heart.quality + OFFER_GAIN);
        heart.awaiting = null;
        heart.awaitingUntil = -1;
        heart.burst(heartPos.above(), 0xD0473B);
    }

    /** The Sacrificer opens a cage when the Heart asks for the creature. */
    public static boolean offerCage(Level level, BlockPos cagePos, Player player) {
        RiteHeartBlockEntity heart = covering(level, cagePos);
        if (heart == null || heart.phase != Phase.CHANNEL || heart.cageUntil < heart.timer || heart.cageCreature != null) {
            return false;
        }
        if (!heart.has(player, RiteRole.SACRIFICER)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.not_your_task", RiteRole.SACRIFICER.displayName()), true);
            return true;
        }
        if (!(level.getBlockEntity(cagePos) instanceof RiteCageBlockEntity cage) || !cage.occupied() || heart.rite == null
                || cage.value() < heart.rite.cageValue()) {
            return false;
        }
        heart.takeCage(cage);
        return true;
    }

    void takeCage(RiteCageBlockEntity cage) {
        cageCreature = cage.creature().copy();
        cageOtherworldly = cage.otherworldly();
        quality = Math.min(100F, quality + Math.min(15F, cage.value() * 1.5F));
        cageUntil = -1;
        burst(cage.getBlockPos(), 0x6F2927);
        cage.consume();
    }

    java.util.Optional<OfferingPedestalBlockEntity> pedestal() {
        return level != null && level.getBlockEntity(worldPosition.above()) instanceof OfferingPedestalBlockEntity p
                ? java.util.Optional.of(p) : java.util.Optional.empty();
    }

    // ------------------------------------------------------------------ start

    /** The Book of Sigils on the Heart: sneaking - a report; otherwise start (idle) or close (closing). */
    public void use(Player player, boolean report) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (phase == Phase.CLOSING) {
            if (has(player, RiteRole.READER)) {
                finish(false);
            } else {
                player.displayClientMessage(Component.translatable("arcane_artifacts.rite.not_your_task", RiteRole.READER.displayName()), true);
            }
            return;
        }
        if (phase == Phase.CHANNEL) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.running", Math.round(quality)), true);
            return;
        }
        RiteSite scanned = RiteSite.scan(server, worldPosition);
        if (report) {
            report(player, scanned);
            return;
        }
        String problem = tryStart(server, scanned, player);
        if (problem != null) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.cannot." + problem), false);
        }
    }

    /** What the site would do now, for the Book. */
    void report(Player player, RiteSite scanned) {
        if (scanned.size == 0) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.cannot.no_boundary"), false);
            return;
        }
        player.displayClientMessage(Component.translatable("arcane_artifacts.rite.report_site", scanned.size, scanned.size, scanned.participants()), false);
        for (String problem : scanned.problems) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.rite.cannot." + problem), false);
        }
        scanned.places.forEach((place, reading) -> {
            if (!reading.empty()) {
                Component what = reading.sigil() == null ? Component.translatable("arcane_artifacts.rite.sigil_unknown")
                        : reading.valid() ? reading.sigil().displayName()
                        : Component.translatable("arcane_artifacts.rite.sigil_wrong_color", reading.sigil().displayName());
                player.displayClientMessage(Component.translatable("arcane_artifacts.rite.report_place", placeName(place), what), false);
            }
        });
        RiteDefinition called = Rites.calledBy(scanned);
        RiteModifiers m = RiteModifiers.of(scanned);
        player.displayClientMessage(called == null ? Component.translatable("arcane_artifacts.rite.report_no_rite")
                : Component.translatable("arcane_artifacts.rite.report_rite", called.displayName()), false);
        player.displayClientMessage(Component.translatable("arcane_artifacts.rite.report_quality", Math.round(RiteQuality.start(scanned, m)),
                scanned.candlesLit, scanned.lanterns, scanned.censer ? "+" : "-", m.total()), false);
    }

    public static Component placeName(RiteSite.Place place) {
        if (place.equals(RiteSite.Place.MAIN)) {
            return Component.translatable("arcane_artifacts.rite.place.main");
        }
        String ns = place.z() < 0 ? "n" : place.z() > 0 ? "s" : "";
        String ew = place.x() < 0 ? "w" : place.x() > 0 ? "e" : "";
        int far = Math.max(Math.abs(place.x()), Math.abs(place.z()));
        return Component.translatable("arcane_artifacts.rite.place." + ns + ew, far);
    }

    /** Players standing on the field. */
    List<Player> onField(RiteSite scanned) {
        List<Player> list = new ArrayList<>();
        if (level == null) {
            return list;
        }
        int h = scanned.half();
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(h, 0, h).expandTowards(0, 4, 0).inflate(0.5))) {
            if (!p.isSpectator() && p.isAlive()) {
                list.add(p);
            }
        }
        return list;
    }

    /** Starts the rite; returns the reason it cannot start, or null. Participants are passed explicitly by tests. */
    @Nullable
    public String tryStart(ServerLevel server, RiteSite scanned, Player starter) {
        List<Player> found = onField(scanned);
        if (!found.contains(starter)) {
            found.add(0, starter);
        }
        return tryStart(server, scanned, starter, found);
    }

    @Nullable
    public String tryStart(ServerLevel server, RiteSite scanned, Player starter, List<Player> players) {
        if (!scanned.ok()) {
            return scanned.problems.isEmpty() ? "no_boundary" : scanned.problems.get(0);
        }
        RiteDefinition called = Rites.calledBy(scanned);
        if (called == null) {
            return "no_rite";
        }
        if (scanned.size < called.minField()) {
            return "field_small";
        }
        boolean solo = starter.isCreative() && Config.RITE_CREATIVE_SOLO.get();
        if (players.size() < scanned.participants() && !solo) {
            return "participants";
        }
        RiteModifiers m = RiteModifiers.of(scanned);
        OfferingPedestalBlockEntity pedestal = pedestal().orElse(null);
        if (pedestal == null) {
            return "pedestal";
        }
        ItemStack laid = pedestal.item();
        boolean metal = called.kind() == RiteDefinition.Kind.TRANSMUTE && !called.itemMap().isEmpty();
        if (metal ? !called.itemMap().containsKey(laid.getItem())
                : !(laid.is(called.offering().item()) && laid.getCount() >= called.offering().count() + m.extraOffering())) {
            return "offering";
        }
        BlockPos cage = null;
        if (called.needsCage()) {
            cage = findCage(scanned, called.cageValue());
            if (cage == null) {
                return "cage";
            }
        }
        begin(server, scanned, called, m, players, cage);
        return null;
    }

    @Nullable
    BlockPos findCage(RiteSite scanned, int min) {
        if (level == null) {
            return null;
        }
        int h = scanned.half();
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-h, 1, -h), worldPosition.offset(h, 2, h))) {
            if (level.getBlockEntity(pos) instanceof RiteCageBlockEntity c && c.occupied() && c.value() >= min) {
                return pos.immutable();
            }
        }
        return null;
    }

    void begin(ServerLevel server, RiteSite scanned, RiteDefinition called, RiteModifiers m, List<Player> players, @Nullable BlockPos cage) {
        site = scanned;
        rite = called;
        mods = m;
        lastHalf = scanned.half();
        participants.clear();
        participants.addAll(players);
        roles.clear();
        List<EnumSet<RiteRole>> assigned = RiteRole.assign(participants.size());
        for (int i = 0; i < participants.size(); i++) {
            roles.put(participants.get(i).getUUID(), assigned.get(i));
        }
        quality = RiteQuality.start(scanned, m) + gearBonus(players);
        int seconds = called.channelSeconds() > 0 ? called.channelSeconds() : Config.RITE_CHANNEL_SECONDS.get();
        duration = seconds * 20 + m.extraChannelTicks();
        timer = 0;
        readProgress = 0;
        fading = null;
        nextFade = FADE_EVERY / 2;
        mainMisses = 0;
        offerIndex = 0;
        awaiting = null;
        awaitingUntil = -1;
        cageUntil = -1;
        cageAsked = false;
        cagePos = cage;
        cageCreature = null;
        cageOtherworldly = false;
        waves = 0;
        candles.clear();
        int h = scanned.half();
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-h - 1, 1, -h - 1), worldPosition.offset(h + 1, 4, h + 1))) {
            BlockState state = server.getBlockState(pos);
            if (state.getBlock() instanceof CandleBlock && state.getValue(CandleBlock.LIT)) {
                candles.add(pos.immutable());
            }
        }
        boolean metal = called.kind() == RiteDefinition.Kind.TRANSMUTE && !called.itemMap().isEmpty();
        if (!metal) {
            pedestal().ifPresent(p -> p.burn(called.offering().count() + m.extraOffering()));
        }
        float blood = m.bloodPrice();
        for (Player p : participants) {
            if (blood > 0) {
                p.hurt(server.damageSources().magic(), CultGearItem.wears(p, CultGearItem.Piece.ROBE) ? blood / 2 : blood);
            }
            p.displayClientMessage(Component.translatable("arcane_artifacts.rite.started", called.displayName(), roleLine(p)), false);
        }
        phase = Phase.CHANNEL;
        ACTIVE.add(this);
        server.playSound(null, worldPosition, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 1.2F, 0.6F);
        burst(worldPosition.above(), 0x8E1212);
        setChanged();
    }

    /** The Cultist's vestments: +3% for every participant in the full set, up to +15%. */
    public static float gearBonus(List<Player> players) {
        int full = 0;
        for (Player p : players) {
            if (CultGearItem.fullSet(p)) {
                full++;
            }
        }
        return Math.min(15F, CultGearItem.SET_QUALITY * full);
    }

    Component roleLine(Player player) {
        EnumSet<RiteRole> set = roles.getOrDefault(player.getUUID(), EnumSet.noneOf(RiteRole.class));
        List<Component> names = new ArrayList<>();
        set.forEach(r -> names.add(r.displayName()));
        return net.minecraft.network.chat.ComponentUtils.formatList(names, Component.literal(", "));
    }

    // ------------------------------------------------------------------ the channel

    public static void serverTick(Level level, BlockPos pos, BlockState state, RiteHeartBlockEntity heart) {
        heart.tick();
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (taint > 0 && server.getGameTime() % 100 == 0) {
            spreadTaint(server, 3);
        }
        if (phase == Phase.IDLE) {
            return;
        }
        participants.removeIf(p -> !p.isAlive() || p.isRemoved() && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer));
        if (participants.isEmpty()) {
            fail("deserted");
            return;
        }
        timer++;
        if (phase == Phase.CLOSING) {
            if (timer > closeUntil) {
                finish(true);
            }
            sync();
            return;
        }
        channel(server);
        if (timer % 5 == 0) {
            sync();
        }
    }

    void channel(ServerLevel server) {
        if (rite == null || site == null) {
            fail("deserted");
            return;
        }
        Player reader = holder(RiteRole.READER);
        if (reader != null && reader.isUsingItem() && reader.getUseItem().getItem() instanceof SigilBookItem
                && reader.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 25) {
            readProgress++;
            if (timer % 2 == 0 && CultGearItem.wears(reader, CultGearItem.Piece.MASK)) {
                readProgress++;
            }
        }
        // fading sigils
        if (fading != null && timer > fadingUntil) {
            quality -= TRACE_MISS;
            if (fading.equals(RiteSite.Place.MAIN) && ++mainMisses >= 2) {
                fail("main_faded");
                return;
            }
            fading = null;
        }
        if (fading == null && timer >= nextFade && timer < duration - FADE_WINDOW) {
            List<RiteSite.Place> drawn = new ArrayList<>();
            site.places.forEach((place, reading) -> {
                if (reading.valid()) {
                    drawn.add(place);
                }
            });
            if (!drawn.isEmpty()) {
                fading = drawn.get(server.random.nextInt(drawn.size()));
                Player tracer = holder(RiteRole.TRACER);
                fadingUntil = timer + FADE_WINDOW + (CultGearItem.wears(tracer, CultGearItem.Piece.GLOVES) ? CultGearItem.GLOVES_EXTRA : 0);
                if (tracer != null) {
                    tracer.displayClientMessage(Component.translatable("arcane_artifacts.rite.trace_now", placeName(fading)), true);
                }
            }
            nextFade = timer + FADE_EVERY;
        }
        if (fading != null && timer % 5 == 0) {
            BlockPos c = site.corner(fading);
            server.sendParticles(new DustParticleOptions(new Vector3f(0.95F, 0.93F, 0.9F), 1.2F), c.getX() + 1.5, c.getY() + 0.2, c.getZ() + 1.5,
                    6, 1.0, 0.05, 1.0, 0);
        }
        // wind and candles
        boolean calm = CultGearItem.wears(holder(RiteRole.KEEPER), CultGearItem.Piece.SANDALS) && (timer / WIND_EVERY) % 2 == 1;
        if (timer % WIND_EVERY == 0 && !candles.isEmpty() && !calm) {
            BlockPos pick = candles.get(server.random.nextInt(candles.size()));
            BlockState state = server.getBlockState(pick);
            if (state.getBlock() instanceof CandleBlock && state.getValue(CandleBlock.LIT)) {
                server.setBlock(pick, state.setValue(CandleBlock.LIT, false), 3);
                server.playSound(null, pick, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        if (timer % 20 == 0) {
            int out = 0;
            for (BlockPos c : candles) {
                BlockState state = server.getBlockState(c);
                if (!(state.getBlock() instanceof CandleBlock) || !state.getValue(CandleBlock.LIT)) {
                    out++;
                }
            }
            quality -= 0.25F * out;
            for (Player p : participants) {
                EnumSet<RiteRole> set = roles.get(p.getUUID());
                boolean guardOnly = set != null && set.size() == 1 && set.contains(RiteRole.GUARD);
                int reach = site.half() + (guardOnly ? 8 : 0);
                if (Math.abs(p.getX() - worldPosition.getX() - 0.5) > reach + 0.5 || Math.abs(p.getZ() - worldPosition.getZ() - 0.5) > reach + 0.5) {
                    quality -= 1F;
                }
            }
        }
        // offerings asked during the channel
        int extras = rite.extraOfferings().size();
        if (offerIndex < extras && awaiting == null && timer >= duration * (offerIndex + 1) / (extras + 1)) {
            awaiting = rite.extraOfferings().get(offerIndex++);
            Player sacrificer = holder(RiteRole.SACRIFICER);
            awaitingUntil = timer + OFFER_WINDOW + (CultGearItem.wears(sacrificer, CultGearItem.Piece.BELT) ? CultGearItem.BELT_EXTRA : 0);
            if (sacrificer != null) {
                sacrificer.displayClientMessage(Component.translatable("arcane_artifacts.rite.offer_now", awaiting.count(), awaiting.item().getDescription()), false);
            }
        }
        if (awaiting != null && timer > awaitingUntil) {
            quality -= OFFER_MISS;
            awaiting = null;
            awaitingUntil = -1;
        }
        // the caged creature, half way through
        if (rite.needsCage() && !cageAsked && timer >= duration / 2) {
            cageAsked = true;
            Player sacrificer = holder(RiteRole.SACRIFICER);
            cageUntil = timer + OFFER_WINDOW + (CultGearItem.wears(sacrificer, CultGearItem.Piece.BELT) ? CultGearItem.BELT_EXTRA : 0);
            if (sacrificer != null) {
                sacrificer.displayClientMessage(Component.translatable("arcane_artifacts.rite.cage_now"), false);
            }
        }
        if (cageAsked && cageCreature == null && cageUntil >= 0 && timer > cageUntil) {
            cageUntil = -1;
            if (rite.kind() == RiteDefinition.Kind.TRANSMUTE && rite.itemMap().isEmpty()) {
                fail("no_cage");
                return;
            }
            quality -= CAGE_MISS;
        }
        // Echoes over the boundary
        int[] at = {duration / 4, duration * 11 / 20, duration * 4 / 5};
        if (waves < at.length && timer >= at[waves]) {
            waves++;
            wave(server, Math.max(0, 1 + participants.size() / 2 - mods.fewerEchoes()));
        }
        quality = Math.max(0F, Math.min(100F, quality));
        if (timer >= duration) {
            phase = Phase.CLOSING;
            closeUntil = timer + CLOSE_TICKS;
            Player r = holder(RiteRole.READER);
            if (r != null) {
                r.displayClientMessage(Component.translatable("arcane_artifacts.rite.close_now"), false);
            }
            server.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.5F, 0.5F);
        }
    }

    void wave(ServerLevel server, int count) {
        if (site == null) {
            return;
        }
        int d = site.half() + 2;
        for (int i = 0; i < count; i++) {
            Echo echo = ModEntities.ECHO.get().create(server);
            if (echo == null) {
                continue;
            }
            int side = server.random.nextInt(4);
            int along = server.random.nextInt(d * 2 + 1) - d;
            int dx = side == 0 ? along : side == 1 ? along : side == 2 ? -d : d;
            int dz = side == 0 ? -d : side == 1 ? d : along;
            BlockPos at = worldPosition.offset(dx, 1, dz);
            echo.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, server.random.nextFloat() * 360, 0);
            server.addFreshEntity(echo);
            server.sendParticles(ParticleTypes.SCULK_SOUL, echo.getX(), echo.getY() + 1, echo.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        }
    }

    // ------------------------------------------------------------------ the end

    void finish(boolean late) {
        if (!(level instanceof ServerLevel server) || rite == null || site == null) {
            reset();
            return;
        }
        float read = duration <= 0 ? 0 : Math.min(1F, readProgress / (duration * 0.6F));
        quality += read * 20F - 10F;
        if (late) {
            quality -= 10F;
        }
        quality = Math.max(0F, Math.min(100F, quality));
        RiteTier tier = RiteTier.of(quality);
        if (tier == RiteTier.BROKEN) {
            fail("broken");
            return;
        }
        Player reader = holder(RiteRole.READER);
        BlockPos at = worldPosition.above();
        boolean otherworldly = rite.otherworldly();
        switch (rite.kind()) {
            case SUMMON -> {
                int life = Math.round(rite.lifeSeconds() * 20 * mods.lifeFactor());
                float loot = tier.loot + mods.extraLoot();
                boolean hostile = late && !mods.loyal();
                for (RiteDefinition.Summon s : rite.summons()) {
                    int count = Math.max(1, Math.round(s.count() * tier.count + mods.extraCount()));
                    RiteSummons.raise(server, at, s.type(), count, life, loot, mods.empowered(), mods.eliteChance(),
                            mods.loyal() ? reader : null, hostile, participants);
                }
            }
            case CREATE -> {
                for (RiteDefinition.Offering o : rite.creates()) {
                    int count = Math.max(1, Math.round(o.count() * tier.count));
                    Containers.dropItemStack(server, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, new ItemStack(o.item(), count));
                }
            }
            case TRANSMUTE -> {
                if (cageCreature != null) {
                    ResourceLocation from = ResourceLocation.tryParse(cageCreature.getString("id"));
                    ResourceLocation to = from == null ? null : rite.entityMap().get(from);
                    if (to != null) {
                        RiteSummons.transmute(server, at.offset(2, 0, 0), cageCreature, to);
                        var toType = BuiltInRegistries.ENTITY_TYPE.getOptional(to);
                        otherworldly |= cageOtherworldly || toType.map(CageValue::otherworldly).orElse(false);
                    } else {
                        RiteCageItem.release(server, wrap(cageCreature), at.offset(2, 0, 0));
                        broadcast(Component.translatable("arcane_artifacts.rite.transmute_nothing"));
                    }
                } else {
                    OfferingPedestalBlockEntity pedestal = pedestal().orElse(null);
                    if (pedestal != null && !pedestal.item().isEmpty()) {
                        Item to = rite.itemMap().get(pedestal.item().getItem());
                        if (to != null) {
                            int count = Math.max(1, Math.round(pedestal.item().getCount() * tier.count / 2F));
                            pedestal.setItem(new ItemStack(to, count));
                        }
                    }
                }
            }
            case CLEANSE -> cleanse(server, rite.cleanse());
        }
        giveReward(server, rite, tier, at);
        if (otherworldly) {
            taint++;
            spreadTaint(server, 24);
        }
        broadcast(Component.translatable("arcane_artifacts.rite.done", rite.displayName(), Math.round(quality), tier.displayName()));
        // every participant is paid in experience: 10 / 20 / 35 / 60 by the tier
        int xp = switch (tier) {
            case WEAK -> 10;
            case STEADY -> 20;
            case FLAWLESS -> 35;
            case PERFECT -> 60;
            default -> 0;
        };
        for (Player p : participants) {
            if (xp > 0) {
                net.minecraft.world.entity.ExperienceOrb.award(server, p.position(), xp);
            }
        }
        server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 0.7F);
        burst(at, 0xFAF7F3);
        reset();
    }

    /**
     * The rite's gift on the altar: its loot table ({@code <namespace>:rites/<rite>}) rolled once for a Weak rite, twice for
     * a Steady one, three times Flawless, four times Perfect, and once more for each Vein. Returns how many stacks fell.
     */
    public int giveReward(ServerLevel server, RiteDefinition done, RiteTier tier, BlockPos at) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(done.id().getNamespace(), "rites/" + done.id().getPath()));
        var table = server.getServer().reloadableRegistries().getLootTable(key);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(at))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int rolls = tier.ordinal() + mods.count(Sigil.ZHILA);
        int dropped = 0;
        for (int i = 0; i < rolls; i++) {
            for (ItemStack stack : table.getRandomItems(params)) {
                Containers.dropItemStack(server, at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, stack);
                dropped++;
            }
        }
        return dropped;
    }

    static CompoundTag wrap(CompoundTag creature) {
        CompoundTag tag = new CompoundTag();
        tag.put(RiteCageItem.CREATURE, creature);
        return tag;
    }

    void fail(String reason) {
        broadcast(Component.translatable("arcane_artifacts.rite.failed." + reason));
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                    40, 0.6, 0.6, 0.6, 0.02);
            server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
            if (rite != null && rite.otherworldly()) {
                taint++;
                spreadTaint(server, 12);
            }
        }
        quality = 0;
        reset();
    }

    void broadcast(Component message) {
        for (Player p : participants) {
            p.displayClientMessage(message, false);
        }
    }

    void reset() {
        phase = Phase.IDLE;
        ACTIVE.remove(this);
        sync();
        participants.clear();
        roles.clear();
        candles.clear();
        fading = null;
        awaiting = null;
        rite = null;
        site = null;
        setChanged();
    }

    // ------------------------------------------------------------------ corruption

    /** Turns up to {@code tries} random grass and dirt blocks around the site into corrupted soil. Returns how many. */
    public int spreadTaint(ServerLevel server, int tries) {
        int radius = lastHalf + 2 + 3 * taint;
        int done = 0;
        for (int i = 0; i < tries; i++) {
            int dx = server.random.nextInt(radius * 2 + 1) - radius;
            int dz = server.random.nextInt(radius * 2 + 1) - radius;
            if (Math.abs(dx) <= lastHalf + 1 && Math.abs(dz) <= lastHalf + 1) {
                continue;
            }
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos p = worldPosition.offset(dx, dy, dz);
                if (CorruptedSoilBlock.corruptible(server.getBlockState(p)) && !server.getBlockState(p.above()).isSolid()) {
                    server.setBlock(p, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState(), 3);
                    done++;
                    break;
                }
            }
        }
        setChanged();
        return done;
    }

    /** The Rite of Cleansing: less taint, and the corrupted soil around turns back into dirt. Returns how many blocks. */
    public int cleanse(ServerLevel server, int amount) {
        taint = Math.max(0, taint - amount);
        int radius = lastHalf + 26;
        int cleaned = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-radius, -5, -radius), worldPosition.offset(radius, 4, radius))) {
            if (server.getBlockState(p).is(ModBlocks.CORRUPTED_SOIL.get())) {
                server.setBlock(p, Blocks.DIRT.defaultBlockState(), 3);
                cleaned++;
            }
        }
        setChanged();
        return cleaned;
    }

    // ------------------------------------------------------------------ effects, HUD, saving

    void burst(BlockPos pos, int rgb) {
        if (level instanceof ServerLevel server) {
            Vector3f color = new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F);
            server.sendParticles(new DustParticleOptions(color, 1.6F), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.5, 0.4, 0.5, 0);
        }
    }

    void sync() {
        for (Player p : participants) {
            if (p instanceof ServerPlayer sp && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
                PacketDistributor.sendToPlayer(sp, statusFor(p));
            }
        }
    }

    RiteNetwork.RitePayload statusFor(Player p) {
        if (phase == Phase.IDLE || rite == null) {
            return RiteNetwork.RitePayload.NONE;
        }
        int task = 0;
        int arg = 0;
        int left = 0;
        if (phase == Phase.CLOSING) {
            task = has(p, RiteRole.READER) ? 4 : 0;
            left = closeUntil - timer;
        } else if (fading != null && has(p, RiteRole.TRACER)) {
            task = 1;
            arg = (fading.x() + 8) * 17 + (fading.z() + 8);
            left = fadingUntil - timer;
        } else if (awaiting != null && has(p, RiteRole.SACRIFICER)) {
            task = 2;
            arg = BuiltInRegistries.ITEM.getId(awaiting.item());
            left = awaitingUntil - timer;
        } else if (cageUntil >= timer && cageCreature == null && has(p, RiteRole.SACRIFICER)) {
            task = 3;
            left = cageUntil - timer;
        } else if (has(p, RiteRole.READER)) {
            task = 5;
        }
        int mask = 0;
        for (RiteRole r : roles.getOrDefault(p.getUUID(), EnumSet.noneOf(RiteRole.class))) {
            mask |= 1 << r.ordinal();
        }
        return new RiteNetwork.RitePayload(true, Math.round(quality), Math.max(0, (phase == Phase.CLOSING ? closeUntil : duration) - timer),
                mask, task, arg, Math.max(0, left), Math.round(100F * Math.min(1F, readProgress / Math.max(1F, duration * 0.6F))));
    }

    @Override
    public void setRemoved() {
        ACTIVE.remove(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Taint", taint);
        tag.putInt("LastHalf", lastHalf);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        taint = tag.getInt("Taint");
        lastHalf = tag.contains("LastHalf") ? tag.getInt("LastHalf") : 4;
    }

    /** For the log line when a rite starts (debugging). */
    @Override
    public String toString() {
        return ArcaneArtifacts.MODID + ":rite_heart@" + worldPosition.toShortString() + " " + phase;
    }
}
