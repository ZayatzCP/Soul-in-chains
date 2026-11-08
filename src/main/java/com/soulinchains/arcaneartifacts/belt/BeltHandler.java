package com.soulinchains.arcaneartifacts.belt;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Iterator;
import java.util.Optional;

/**
 * Automatic collection of the Belt of Bottomless Pouches: items of a kind the belt already holds go
 * straight into their pouch, whether they are picked up from the ground or dropped by a block the
 * wearer breaks (they never touch the ground). Other items behave normally.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class BeltHandler {
    private BeltHandler() {
    }

    public static void openMenu(ServerPlayer player) {
        if (CurioHelper.findEquipped(player, ModItems.BOTTOMLESS_BELT.get()).isEmpty()) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.belt.not_worn"), true);
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BeltMenu(id, inventory),
                Component.translatable("item.arcane_artifacts.bottomless_belt")));
    }

    /** Puts as much of the stack as possible into the belt; returns true if everything went in. */
    public static boolean store(Player player, ItemStack stack) {
        Optional<ItemStack> beltStack = CurioHelper.findEquipped(player, ModItems.BOTTOMLESS_BELT.get());
        if (beltStack.isEmpty() || stack.isEmpty()) {
            return false;
        }
        BeltContents contents = BottomlessBeltItem.contents(beltStack.get());
        int pouch = contents.pouchFor(stack);
        if (pouch < 0) {
            return false;
        }
        beltStack.get().set(ModComponents.BELT_CONTENTS, contents.add(pouch, stack, stack.getCount()));
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity item = event.getItemEntity();
        if (player.level().isClientSide || item.hasPickUpDelay() || item.getItem().isEmpty()) {
            return;
        }
        ItemStack stack = item.getItem();
        int count = stack.getCount();
        if (store(player, stack)) {
            player.take(item, count);
            item.discard();
            event.setCanPickup(TriState.FALSE);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.3f, 1.2f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || event.isCanceled()) {
            return;
        }
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity drop = it.next();
            if (store(player, drop.getItem())) {
                it.remove();
            }
        }
    }
}
