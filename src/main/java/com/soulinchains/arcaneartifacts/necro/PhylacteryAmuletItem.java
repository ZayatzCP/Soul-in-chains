package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.Optional;

/**
 * Phylactery Amulet (necklace, Shackled set): a piece of your soul kept apart. Bind it to a place (sneak + right click a
 * block); when you would die wearing it, you are torn back to that place instead, alive, with everything you carry. Then
 * it sleeps for {@code phylactery_cooldown_days}. While it is bound it slowly feeds Corruption. Overseer rank.
 * <p>
 * Right click in the air changes its look (crow skull, caged heart, hourglass, black stone, rag doll) - only the look.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public class PhylacteryAmuletItem extends ArcaneAccessoryItem {
    /** While bound, +1 Corruption per this many ticks of wearing it. */
    public static final int CORRUPTION_TICKS = 12000;

    public PhylacteryAmuletItem() {
        super(new Item.Properties().rarity(Rarity.EPIC).component(ModComponents.PHYLACTERY, Phylactery.EMPTY), 2);
    }

    public static Phylactery state(ItemStack stack) {
        return stack.getOrDefault(ModComponents.PHYLACTERY, Phylactery.EMPTY);
    }

    public static int style(ItemStack stack) {
        return state(stack).style();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            Phylactery next = state(stack).withStyle(style(stack) + 1);
            stack.set(ModComponents.PHYLACTERY, next);
            player.displayClientMessage(Component.translatable("arcane_artifacts.phylactery.style",
                    Component.translatable("arcane_artifacts.phylactery.style." + next.style())), true);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.6F + next.style() * 0.15F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            BlockPos spot = context.getClickedPos().above();
            bind(context.getItemInHand(), GlobalPos.of(context.getLevel().dimension(), spot));
            player.displayClientMessage(Component.translatable("arcane_artifacts.phylactery.bound", spot.getX(), spot.getY(), spot.getZ()), true);
            context.getLevel().playSound(null, spot, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    public static void bind(ItemStack stack, GlobalPos pos) {
        stack.set(ModComponents.PHYLACTERY, state(stack).boundTo(pos));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide && player.tickCount % CORRUPTION_TICKS == 0
                && state(stack).bound().isPresent()) {
            Corruption.add(player, 1.0F);
        }
    }

    private static long now(Level level) {
        MinecraftServer server = level.getServer();
        return server != null ? server.overworld().getGameTime() : level.getGameTime();
    }

    public static long cooldownTicks() {
        return Config.PHYLACTERY_COOLDOWN_DAYS.get() * 24000L;
    }

    /** Death averted: the wearer is torn back to the bound place, alive, with everything. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || event.getSource().is(DamageTypes.GENERIC_KILL)) {
            return;
        }
        Optional<ItemStack> worn = CurioHelper.findEquipped(player, ModItems.PHYLACTERY_AMULET.get());
        if (worn.isEmpty() || Necro.rank(player) < Necro.OVERSEER) {
            return;
        }
        ItemStack stack = worn.get();
        Phylactery state = state(stack);
        long now = now(player.level());
        if (state.bound().isEmpty() || state.readyAt() > now) {
            return;
        }
        GlobalPos home = state.bound().get();
        ServerLevel target = player.level().getServer() == null ? null : player.level().getServer().getLevel(home.dimension());
        if (target == null) {
            return;
        }
        event.setCanceled(true);
        ServerLevel from = (ServerLevel) player.level();
        from.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        player.setHealth(player.getMaxHealth() * 0.5F);
        player.removeAllEffects();
        player.clearFire();
        player.fallDistance = 0;
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        double x = home.pos().getX() + 0.5;
        double y = home.pos().getY();
        double z = home.pos().getZ() + 0.5;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(target, x, y, z, player.getYRot(), player.getXRot());
        } else if (target == player.level()) {
            player.teleportTo(x, y, z);
        }
        stack.set(ModComponents.PHYLACTERY, state.usedUntil(now + cooldownTicks()));
        target.sendParticles(ParticleTypes.SCULK_SOUL, x, y + 1, z, 30, 0.4, 0.8, 0.4, 0.05);
        target.playSound(null, home.pos(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 0.5F);
        player.displayClientMessage(Component.translatable("arcane_artifacts.phylactery.saved").withStyle(ChatFormatting.DARK_AQUA), false);
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Phylactery state = state(stack);
        tooltip.add(Component.translatable("arcane_artifacts.phylactery.style",
                Component.translatable("arcane_artifacts.phylactery.style." + state.style())).withStyle(ChatFormatting.DARK_PURPLE));
        if (state.bound().isPresent()) {
            BlockPos pos = state.bound().get().pos();
            tooltip.add(Component.translatable("arcane_artifacts.phylactery.bound_to", pos.getX(), pos.getY(), pos.getZ(),
                    state.bound().get().dimension().location().getPath()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("arcane_artifacts.phylactery.unbound").withStyle(ChatFormatting.RED));
        }
        Level level = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? ClientTime.level() : null;
        if (level != null && state.readyAt() > level.getGameTime()) {
            long minutes = Math.max(1, (state.readyAt() - level.getGameTime()) / 1200);
            tooltip.add(Component.translatable("arcane_artifacts.phylactery.sleeping", minutes).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Client-only access to the client level, kept in a nested class so servers never load it. */
    private static final class ClientTime {
        static Level level() {
            return com.soulinchains.arcaneartifacts.client.ClientAccess.level();
        }
    }
}
