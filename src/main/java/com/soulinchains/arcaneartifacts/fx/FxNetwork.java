package com.soulinchains.arcaneartifacts.fx;

import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;

/**
 * Draws a styled Ars particle line on the clients with one packet: the client walks an emitter along
 * the line in short steps, so trail motions (whose particle count is per tick) look dense and continuous.
 */
public final class FxNetwork {
    public record LinePayload(TimelineEntryData entry, Vector3f from, Vector3f to) implements CustomPacketPayload {
        public static final Type<LinePayload> TYPE = new Type<>(ArcaneArtifacts.id("fx_line"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LinePayload> STREAM_CODEC = StreamCodec.composite(
                TimelineEntryData.STREAM, LinePayload::entry,
                ByteBufCodecs.VECTOR3F, LinePayload::from,
                ByteBufCodecs.VECTOR3F, LinePayload::to,
                LinePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final double STEP = 0.35;

    private FxNetwork() {
    }

    public static void sendLine(ServerLevel level, TimelineEntryData entry, Vec3 from, Vec3 to) {
        Vec3 mid = from.add(to).scale(0.5);
        PacketDistributor.sendToPlayersNear(level, null, mid.x, mid.y, mid.z, 96 + from.distanceTo(to) / 2,
                new LinePayload(entry, from.toVector3f(), to.toVector3f()));
    }

    public static void handleLine(LinePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            Vec3 from = new Vec3(payload.from());
            Vec3 to = new Vec3(payload.to());
            Vec3[] pos = {from};
            ParticleEmitter emitter = new ParticleEmitter(() -> pos[0], () -> Vec2.ZERO, payload.entry());
            emitter.previousPosition = from;
            int steps = Math.max(1, Math.min(400, (int) Math.ceil(from.distanceTo(to) / STEP)));
            for (int i = 1; i <= steps; i++) {
                pos[0] = from.add(to.subtract(from).scale(i / (double) steps));
                emitter.tick(level);
            }
        });
    }
}
