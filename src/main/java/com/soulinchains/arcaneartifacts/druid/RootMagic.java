package com.soulinchains.arcaneartifacts.druid;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Root Magic, the Druid's school, and the Druid's gear: a herbalist-alchemist who gathers what grows and
 * brews it. Also "living ground" (grass, moss, leaves, dirt, mycelium), which the Moss Elk draws on.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RootMagic {
    private RootMagic() {
    }

    /** Grass, moss, leaves, dirt, mycelium, or standing in grass and flowers. */
    public static boolean onLivingGround(Entity entity) {
        BlockPos below = entity.getBlockPosBelowThatAffectsMyMovement();
        BlockState state = entity.level().getBlockState(below);
        return state.is(BlockTags.DIRT) || state.is(BlockTags.LEAVES) || state.is(Blocks.MOSS_CARPET)
                || entity.level().getBlockState(entity.blockPosition()).is(BlockTags.REPLACEABLE_BY_TREES);
    }

    public static boolean wears(LivingEntity entity, net.minecraft.world.item.Item item) {
        return CurioHelper.findEquipped(entity, item).isPresent();
    }

    /** All four pieces of the Druid's gear. */
    public static boolean fullSet(LivingEntity entity) {
        return wears(entity, ModItems.DRUID_MANTLE.get()) && wears(entity, ModItems.DRUID_HAT.get())
                && wears(entity, ModItems.DRUID_GLOVES.get()) && wears(entity, ModItems.DRUID_BOOTS.get());
    }

    /** What a herbalist gathers: mushrooms (magic ones too), cave plants, flowers, berries, crops, nether wart. */
    public static boolean isHerb(BlockState state) {
        return state.getBlock() instanceof com.soulinchains.arcaneartifacts.block.ArcaneMushroomBlock
                || state.getBlock() instanceof com.soulinchains.arcaneartifacts.cave.CavePlantBlock
                || state.getBlock() instanceof MushroomBlock || state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.NETHER_WART) || state.is(Blocks.GLOW_LICHEN)
                || state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT) || state.is(Blocks.MOSS_BLOCK);
    }

    /** The hat: magic mushrooms and herbs within the radius sparkle for the wearer only. Returns how many. */
    public static int sniff(Player player, int radius) {
        if (!(player.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer viewer)) {
            return 0;
        }
        int found = 0;
        BlockPos centre = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius / 2, -radius), centre.offset(radius, radius / 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof com.soulinchains.arcaneartifacts.block.ArcaneMushroomBlock
                    || state.getBlock() instanceof com.soulinchains.arcaneartifacts.cave.CavePlantBlock) {
                level.sendParticles(viewer, ParticleTypes.HAPPY_VILLAGER, true, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
                if (++found >= 40) {
                    break;
                }
            }
        }
        return found;
    }

    /** The gloves: half the time a herb gives its harvest twice over. */
    @SubscribeEvent
    public static void onHarvest(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof LivingEntity breaker) || !isHerb(event.getState())
                || !wears(breaker, ModItems.DRUID_GLOVES.get())) {
            return;
        }
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            if (breaker.getRandom().nextFloat() < DruidGearItem.EXTRA_HARVEST) {
                extra.add(new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
        }
        event.getDrops().addAll(extra);
    }

    /** The mantle (potions last longer) and the full set (a potion may not be used up). */
    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Finish event) {
        LivingEntity drinker = event.getEntity();
        ItemStack used = event.getItem();
        if (drinker.level().isClientSide || !(used.getItem() instanceof PotionItem)) {
            return;
        }
        if (wears(drinker, ModItems.DRUID_MANTLE.get())) {
            lengthen(drinker, used.get(DataComponents.POTION_CONTENTS));
        }
        if (fullSet(drinker) && drinker.getRandom().nextFloat() < DruidGearItem.FREE_POTION) {
            event.setResultStack(used.copyWithCount(1));
        }
    }

    /** Re-applies a potion's lasting effects 30% longer. Returns how many were lengthened. */
    public static int lengthen(LivingEntity drinker, PotionContents contents) {
        if (contents == null) {
            return 0;
        }
        int lengthened = 0;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (!effect.getEffect().value().isInstantenous()) {
                drinker.addEffect(new MobEffectInstance(effect.getEffect(), Math.round(effect.getDuration() * DruidGearItem.POTION_LONGER),
                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
                lengthened++;
            }
        }
        return lengthened;
    }

    /** Druid's Boots: farmland is not trampled. */
    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof LivingEntity living && wears(living, ModItems.DRUID_BOOTS.get())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ the Roots set (1.31)

    /** The Root Magic spell parts: Entangle, Grove, Bee Swarm. */
    public static boolean isRootPart(com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart part) {
        return part == com.soulinchains.arcaneartifacts.glyph.EffectEntangle.INSTANCE || part == com.soulinchains.arcaneartifacts.glyph.EffectGrove.INSTANCE
                || part == com.soulinchains.arcaneartifacts.glyph.EffectBeeSwarm.INSTANCE;
    }

    /** A Root Magic spell: it carries one of the Root glyphs, or it is cast from a druid staff. */
    public static boolean isRootSpell(com.hollingsworth.arsnouveau.api.spell.SpellContext context) {
        if (context.getCasterTool().getItem() instanceof DruidStaffItem) {
            return true;
        }
        return context.getSpell().unsafeList().stream().anyMatch(RootMagic::isRootPart);
    }

    /** The mantle on living ground: mana from the roots and a little life. */
    public static void drawFromRoots(Player player) {
        var mana = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player);
        if (mana != null && mana.getCurrentMana() < mana.getMaxMana()) {
            mana.addMana(DruidGearItem.MANTLE_MANA_PER_SECOND);
        }
        if (player.tickCount % 120 == 0) {
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 130, 0, true, false, true));
        }
    }

    /** Hat: one Extend Time more for Root glyphs. Gloves: one Amplify more. */
    @SubscribeEvent
    public static void onSpellStats(com.hollingsworth.arsnouveau.api.event.SpellModifierEvent event) {
        if (!(event.caster instanceof LivingEntity caster) || event.spellPart == null || !isRootPart(event.spellPart)) {
            return;
        }
        if (wears(caster, ModItems.DRUID_HAT.get())) {
            event.builder.addDurationModifier(1.0);
        }
        if (wears(caster, ModItems.DRUID_GLOVES.get())) {
            event.builder.addAmplification(1.0);
        }
    }

    /** Hat: Root Magic spells cost 20% less. The Staff of the Five Circles: 15% less on top. */
    @SubscribeEvent
    public static void onCost(com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent.Post event) {
        LivingEntity caster = event.context.getUnwrappedCaster();
        if (caster == null || !isRootSpell(event.context)) {
            return;
        }
        float cost = event.currentCost;
        if (wears(caster, ModItems.DRUID_HAT.get())) {
            cost *= 1F - DruidGearItem.HAT_DISCOUNT;
        }
        if (event.context.getCasterTool().getItem() instanceof DruidStaffItem staff && staff.grand()) {
            cost *= 1F - DruidStaffItem.GRAND_DISCOUNT;
        }
        event.currentCost = Math.round(cost);
    }

    /** Gloves: your own blows hit entangled creatures twice as hard. */
    @SubscribeEvent
    public static void onEntangledHit(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && event.getSource().getDirectEntity() == attacker
                && event.getEntity().hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.ENTANGLED) && wears(attacker, ModItems.DRUID_GLOVES.get())) {
            event.setAmount(event.getAmount() * DruidGearItem.GLOVES_ENTANGLED_BONUS);
        }
    }

    private static final String ROOTS_READY = ArcaneArtifacts.MODID + ":roots_ready";

    /** The full set, Roots: a killing blow on living ground roots you instead (once every 5 minutes). */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL) || !fullSet(player) || !onLivingGround(player)) {
            return;
        }
        long now = level.getGameTime();
        if (player.getPersistentData().getLong(ROOTS_READY) > now) {
            return;
        }
        event.setCanceled(true);
        player.getPersistentData().putLong(ROOTS_READY, now + DruidGearItem.ROOTS_COOLDOWN_TICKS);
        player.setHealth(Math.min(player.getMaxHealth(), DruidGearItem.ROOTS_HEAL));
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 60, 4));
        player.addEffect(new MobEffectInstance(com.soulinchains.arcaneartifacts.registry.ModEffects.ENTANGLED, 60, 0));
        level.sendParticles(ParticleTypes.COMPOSTER, player.getX(), player.getY() + 0.5, player.getZ(), 40, 0.5, 0.6, 0.5, 0.05);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ROOTED_DIRT_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("arcane_artifacts.druid.rooted"), true);
    }
}
