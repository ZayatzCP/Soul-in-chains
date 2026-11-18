package com.soulinchains.arcaneartifacts.catto;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Talking to Catto. Using Catto with an empty hand opens a conversation: the server sends what the player is looking
 * at and holding in the second hand, Catto's mood and plans; the client shows Catto's lines and the player's possible
 * answers; each answer that is an order comes back here as a command, and Catto answers in its own words.
 */
public final class CattoDialog {
    private CattoDialog() {
    }

    /** Server -> client: open or update the conversation, with Catto's line (if any). */
    public record DialogPayload(int entityId, CompoundTag context, Optional<Component> say) implements CustomPacketPayload {
        public static final Type<DialogPayload> TYPE = new Type<>(ArcaneArtifacts.id("catto_dialog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DialogPayload::entityId,
                ByteBufCodecs.COMPOUND_TAG, DialogPayload::context,
                ComponentSerialization.OPTIONAL_STREAM_CODEC, DialogPayload::say,
                DialogPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: the player's answer that asks Catto to do something. */
    public record CommandPayload(int entityId, String command, int count, long pos) implements CustomPacketPayload {
        public static final Type<CommandPayload> TYPE = new Type<>(ArcaneArtifacts.id("catto_command"));
        public static final StreamCodec<ByteBuf, CommandPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CommandPayload::entityId,
                ByteBufCodecs.STRING_UTF8, CommandPayload::command,
                ByteBufCodecs.VAR_INT, CommandPayload::count,
                ByteBufCodecs.VAR_LONG, CommandPayload::pos,
                CommandPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** What the conversation needs to know: Catto's state, the block the player looks at, the item in the second hand. */
    public static CompoundTag context(ServerPlayer player, Catto cat) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Nature", cat.nature().name());
        tag.putBoolean("Paused", cat.paused);
        tag.putBoolean("Stay", cat.stay);
        tag.putInt("Bond", cat.bond);
        tag.putInt("Source", cat.source);
        tag.putInt("MaxSource", cat.maxSource());
        tag.putInt("QueueSize", cat.queue.size());
        tag.putInt("QueueCap", cat.queueCap());
        ListTag queue = new ListTag();
        for (int i = 0; i < Math.min(3, cat.queue.size()); i++) {
            queue.add(cat.queue.get(i).save(cat.registryAccess()));
        }
        tag.put("Queue", queue);
        tag.putInt("Storages", cat.storages.size());
        HitResult hit = player.pick(24.0, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = player.level().getBlockState(pos);
            tag.putLong("LookPos", pos.asLong());
            if (!state.isAir() && state.getDestroySpeed(player.level(), pos) >= 0) {
                tag.putString("LookBlock", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            }
        } else {
            tag.putLong("LookPos", player.blockPosition().asLong());
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            tag.put("Offhand", offhand.save(cat.registryAccess()));
        }
        return tag;
    }

    public static void open(ServerPlayer player, Catto cat) {
        cat.playSound(net.minecraft.sounds.SoundEvents.CAT_AMBIENT, 0.8F, 1.1F);
        PacketDistributor.sendToPlayer(player, new DialogPayload(cat.getId(), context(player, cat), Optional.empty()));
    }

    private static Component say(Catto cat, String what, Object... args) {
        return Component.translatable("arcane_artifacts.catto.dialog." + what + "." + cat.nature().key(), args);
    }

    public static void handleCommand(CommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.level().getEntity(payload.entityId()) instanceof Catto cat)
                    || !cat.isOwnedBy(player) || cat.distanceToSqr(player) > 24 * 24) {
                return;
            }
            if ("inventory".equals(payload.command())) {
                cat.openMenu(player);
                return;
            }
            Component reply = run(player, cat, payload);
            PacketDistributor.sendToPlayer(player, new DialogPayload(cat.getId(), context(player, cat), Optional.ofNullable(reply)));
        });
    }

    @Nullable
    private static Component run(ServerPlayer player, Catto cat, CommandPayload payload) {
        BlockPos pos = BlockPos.of(payload.pos());
        boolean near = pos.distSqr(player.blockPosition()) <= 40 * 40;
        int count = Math.max(1, Math.min(9999, payload.count()));
        return switch (payload.command()) {
            case "mine" -> {
                BlockState state = player.level().getBlockState(pos);
                if (!near || state.isAir() || state.getDestroySpeed(player.level(), pos) < 0) {
                    yield say(cat, "confused");
                }
                yield order(player, cat, CattoTask.mine(state.getBlock(), pos, count));
            }
            case "collect" -> near ? order(player, cat, CattoTask.collect(pos, null, ItemStack.EMPTY)) : say(cat, "confused");
            case "explore" -> near ? order(player, cat, CattoTask.explore(pos)) : say(cat, "confused");
            case "fetch", "craft" -> {
                ItemStack sample = player.getOffhandItem();
                if (sample.isEmpty()) {
                    yield say(cat, "no_sample");
                }
                int n = payload.count() <= 0 ? sample.getCount() : count;
                yield order(player, cat, "fetch".equals(payload.command()) ? CattoTask.fetch(sample, n) : CattoTask.craft(sample, n));
            }
            case "give_all" -> {
                cat.giveAllTo(player);
                yield say(cat, "give");
            }
            case "stay" -> {
                cat.stay = true;
                cat.getNavigation().stop();
                cat.changed();
                yield say(cat, "stay");
            }
            case "follow" -> {
                cat.stay = false;
                cat.paused = false;
                cat.brain().resume();
                cat.changed();
                yield say(cat, "follow");
            }
            case "pause" -> {
                cat.paused = true;
                cat.changed();
                yield say(cat, "pause");
            }
            case "resume" -> {
                cat.paused = false;
                cat.stay = false;
                cat.brain().resume();
                cat.changed();
                yield say(cat, "resume");
            }
            case "cancel_first" -> {
                if (!cat.queue.isEmpty()) {
                    cat.queue.remove(0);
                    cat.brain().taskRemoved();
                    cat.changed();
                }
                yield say(cat, "cancelled");
            }
            case "clear" -> {
                cat.queue.clear();
                cat.brain().stop();
                cat.paused = false;
                cat.stay = false;
                cat.changed();
                yield say(cat, "cleared");
            }
            default -> null;
        };
    }

    /** An errand asked aloud: Catto says yes in its own way, or why not. */
    private static Component order(ServerPlayer player, Catto cat, CattoTask task) {
        Component refusal = cat.refusal(task);
        if (refusal != null) {
            return say(cat, "refuse", refusal);
        }
        cat.queue.add(task);
        cat.changed();
        cat.playSound(net.minecraft.sounds.SoundEvents.CAT_AMBIENT, 0.7F, 1.3F);
        return say(cat, "accept", task.describe());
    }
}
