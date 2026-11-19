package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.event.SpellModifierEvent;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Corruption (0-100), the price of the Shackled's power. Every soul spent adds to it (weak 0.5, blazing/void 1,
 * cursed 3, great 5, pure 10); daylight under the open sky slowly washes it away (1 point per 2 minutes); a Ritual
 * Dagger kill takes 1 point off. Stages:
 * <ul>
 *     <li>0-24 Alive.</li>
 *     <li>25-49 Touched: necromancy glyphs a little stronger; villagers dislike you (prices up).</li>
 *     <li>50-74 Shackled: +1 servant, night vision; in sunlight under the open sky you are weakened.</li>
 *     <li>75-99 Unclean: necromancy glyphs much stronger, weak undead leave you alone; you burn in the sun without a
 *     helmet and villagers will not trade.</li>
 * </ul>
 * 100 (Lich) needs a ritual and a phylactery (a later version); until then Corruption stops at 99.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class Corruption {
    public static final float MAX = 99.0F;
    public static final float TOUCHED = 25F;
    public static final float SHACKLED = 50F;
    public static final float UNCLEAN = 75F;
    /** Washed away per 100 ticks of daylight under the open sky (1 point per 2400 ticks). */
    private static final float DAYLIGHT_WASH = 100F / 2400F;

    private Corruption() {
    }

    public static float get(Player player) {
        return player.getData(ModAttachments.CORRUPTION);
    }

    public static void set(Player player, float value) {
        float clamped = Math.max(0F, Math.min(MAX, value));
        float old = get(player);
        player.setData(ModAttachments.CORRUPTION, clamped);
        if (player instanceof ServerPlayer serverPlayer && (int) old != (int) clamped) {
            sync(serverPlayer);
            int before = stage(old);
            int after = stage(clamped);
            if (after > before) {
                serverPlayer.displayClientMessage(Component.translatable("arcane_artifacts.corruption.stage_up",
                        Component.translatable("arcane_artifacts.corruption.stage." + after)), false);
            }
        }
    }

    public static void add(Player player, float amount) {
        set(player, get(player) + amount);
    }

    /** What spending a soul of this kind costs in Corruption. */
    public static float costOf(SoulType type) {
        return switch (type) {
            case PURE -> 10F;
            case GREAT -> 5F;
            case CURSED -> 3F;
            case BLAZING, VOID -> 1F;
            default -> 0.5F;
        };
    }

    public static void spend(Player player, SoulType type) {
        add(player, costOf(type));
    }

    /** 0 Alive, 1 Touched, 2 Shackled, 3 Unclean. */
    public static int stage(float corruption) {
        return corruption >= UNCLEAN ? 3 : corruption >= SHACKLED ? 2 : corruption >= TOUCHED ? 1 : 0;
    }

    public static int stage(Player player) {
        return stage(get(player));
    }

    // ------------------------------------------------------------------ effects

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 100 != 0) {
            return;
        }
        float value = get(player);
        if (value <= 0) {
            return;
        }
        boolean sunlit = player.level().isDay() && player.level().canSeeSky(player.blockPosition().above());
        if (sunlit) {
            set(player, value - DAYLIGHT_WASH);
        }
        int stage = stage(value);
        if (stage >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
            if (sunlit) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, true, true));
            }
        }
        if (stage >= 3 && sunlit && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !player.isInWaterRainOrBubble()) {
            player.igniteForSeconds(4.0F);
        }
    }

    /** Necromancy glyphs grow stronger with Corruption: longer holds, more damage. */
    @SubscribeEvent
    public static void onSpellStats(SpellModifierEvent event) {
        if (!(event.caster instanceof Player player) || event.spellPart == null || !event.spellPart.spellSchools.contains(SpellSchools.NECROMANCY)) {
            return;
        }
        int stage = stage(player);
        if (stage >= 3) {
            event.builder.addDurationModifier(1.0).addDamageModifier(2.0);
        } else if (stage >= 1) {
            event.builder.addDurationModifier(0.5).addDamageModifier(1.0);
        }
    }

    /** Touched: villagers think less of you. Unclean: they will not trade at all. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractVillager villager) || event.getEntity().level().isClientSide) {
            return;
        }
        Player player = event.getEntity();
        int stage = stage(player);
        if (stage >= 3) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            player.displayClientMessage(Component.translatable("arcane_artifacts.corruption.villager_refuses"), true);
        } else if (stage >= 1 && villager instanceof net.minecraft.world.entity.npc.Villager trader) {
            trader.getGossips().add(player.getUUID(), net.minecraft.world.entity.ai.gossip.GossipType.MINOR_NEGATIVE, 5);
        }
    }

    /**
     * Undead that would leave you alone: Unclean - every weak (strength 1) undead; with the Bone Crown - undead weaker
     * than your rank. Bosses never; and undead you hit fight back as usual.
     */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || !(event.getEntity() instanceof Mob mob)
                || !mob.getType().is(EntityTypeTags.UNDEAD) || mob.getLastHurtByMob() == player) {
            return;
        }
        SoulType soul = SoulType.of(mob);
        int strength = soul == null ? 3 : soul.strength;
        boolean crowned = ShackledSet.wearsCrown(player) && strength < Necro.rank(player);
        if (crowned || stage(player) >= 3 && strength <= 1) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ client sync

    public record SyncPayload(float corruption) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(ArcaneArtifacts.id("corruption"));
        public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC = ByteBufCodecs.FLOAT.map(SyncPayload::new, SyncPayload::corruption);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The local player's Corruption, as last told by the server (client side). */
    public static float clientValue;

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncPayload(get(player)));
    }

    public static void handleSync(SyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientValue = payload.corruption());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
