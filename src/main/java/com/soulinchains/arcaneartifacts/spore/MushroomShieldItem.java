package com.soulinchains.arcaneartifacts.spore;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.util.List;

/** Mushroom Shield: every blocked blow bursts into a cloud of spores (at most once every 2 seconds). */
public class MushroomShieldItem extends ShieldItem {
    public static final int CLOUD_COOLDOWN = 40;
    private static final String LAST_CLOUD = "arcane_artifacts_last_spore_cloud";

    public MushroomShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.RED_MUSHROOM_BLOCK) || repair.is(Items.BROWN_MUSHROOM_BLOCK);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.2").withStyle(ChatFormatting.GRAY));
    }

    /** Bursts a spore cloud around {@code blocker} unless one was burst in the last 2 seconds. */
    public static boolean burst(LivingEntity blocker) {
        if (!(blocker.level() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        long last = blocker.getPersistentData().getLong(LAST_CLOUD);
        if (last != 0 && now - last < CLOUD_COOLDOWN) {
            return false;
        }
        blocker.getPersistentData().putLong(LAST_CLOUD, now);
        SporeCloud.spawn(level, blocker);
        return true;
    }

    @EventBusSubscriber(modid = ArcaneArtifacts.MODID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onBlock(LivingShieldBlockEvent event) {
            LivingEntity blocker = event.getEntity();
            if (event.getBlocked() && blocker.getUseItem().getItem() instanceof MushroomShieldItem) {
                burst(blocker);
                if (event.getDamageSource().getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player) && blocker.distanceTo(attacker) < 4) {
                    Spores.infect(attacker, Spores.INFECTION_TICKS);
                }
            }
        }
    }
}
