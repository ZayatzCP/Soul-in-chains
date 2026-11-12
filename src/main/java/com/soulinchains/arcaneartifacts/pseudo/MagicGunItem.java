package com.soulinchains.arcaneartifacts.pseudo;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A Pseudomage firearm. One glyph is set inside it at the Scribes Table: a single effect, no form, no
 * augments. It needs no mana: each shot uses a round from the magazine, and the magazine is loaded
 * (when empty, or crouch + use) with false mana first ({@link FalseMana#DIVISION} per round), then with
 * magic cartridges from the worn bandolier, then from the inventory.
 */
public class MagicGunItem extends Item implements ICasterTool {
    private final int magazine;
    private final int pellets;
    private final float spread;
    private final int dampen;
    private final int fireCooldown;
    private final int reloadTicks;

    public MagicGunItem(int magazine, int pellets, float spread, int dampen, int fireCooldown, int reloadTicks) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(1)));
        this.magazine = magazine;
        this.pellets = pellets;
        this.spread = spread;
        this.dampen = dampen;
        this.fireCooldown = fireCooldown;
        this.reloadTicks = reloadTicks;
    }

    public int magazine() {
        return magazine;
    }

    public static int rounds(ItemStack stack) {
        return stack.getOrDefault(ModComponents.GUN_ROUNDS, 0);
    }

    public static Spell storedSpell(ItemStack stack) {
        SpellCaster caster = stack.get(DataComponentRegistry.SPELL_CASTER);
        return caster == null ? new Spell() : caster.getSpell();
    }

    /** Exactly one effect glyph: no form, no augments. */
    public static boolean isValidGunSpell(Spell spell) {
        List<AbstractSpellPart> parts = spell.unsafeList();
        return parts.size() == 1 && parts.get(0) instanceof AbstractEffect;
    }

    /** What the bullet carries: the glyph, then this gun's Dampen. */
    public Spell bulletSpell(Spell stored) {
        List<AbstractSpellPart> parts = new ArrayList<>(stored.unsafeList());
        for (int i = 0; i < dampen; i++) {
            parts.add(AugmentDampen.INSTANCE);
        }
        return stored.mutable().setRecipe(parts).immutable();
    }

    @Override
    public boolean isScribedSpellValid(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        return isValidGunSpell(spell);
    }

    @Override
    public void sendInvalidMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("arcane_artifacts.gun.invalid"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(gun);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(gun);
        }
        int rounds = rounds(gun);
        if (rounds <= 0 || (player.isShiftKeyDown() && rounds < magazine)) {
            reload(player, gun);
            return InteractionResultHolder.consume(gun);
        }
        Spell spell = storedSpell(gun);
        if (!isValidGunSpell(spell)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.gun.empty_glyph").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(gun);
        }
        fire(player, gun, bulletSpell(spell));
        gun.set(ModComponents.GUN_ROUNDS, rounds - 1);
        player.getCooldowns().addCooldown(this, fireCooldown);
        return InteractionResultHolder.consume(gun);
    }

    private void fire(Player player, ItemStack gun, Spell spell) {
        Level level = player.level();
        for (int i = 0; i < pellets; i++) {
            SpellContext context = new SpellContext(level, spell, player, LivingCaster.from(player), gun.copy());
            BulletEntity bullet = new BulletEntity(level, player, new SpellResolver(context));
            Vec3 eye = player.getEyePosition();
            bullet.setPos(eye.x, eye.y - 0.1, eye.z);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.2F, spread);
            level.addFreshEntity(bullet);
        }
        level.playSound(null, player.blockPosition(), pellets > 1 ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS, pellets > 1 ? 0.6f : 1.0f, pellets > 1 ? 1.6f : 0.8f);
    }

    /** Loads rounds: false mana first, then cartridges from the bandolier, then from the inventory. */
    private void reload(Player player, ItemStack gun) {
        int rounds = rounds(gun);
        int needed = magazine - rounds;
        int loaded = 0;
        while (loaded < needed && FalseMana.spend(player, FalseMana.DIVISION)) {
            loaded++;
        }
        ItemStack bandolier = CurioHelper.findEquipped(player, ModItems.BANDOLIER.get()).orElse(ItemStack.EMPTY);
        if (loaded < needed && !bandolier.isEmpty()) {
            int inside = BandolierItem.cartridges(bandolier);
            int take = Math.min(inside, needed - loaded);
            BandolierItem.setCartridges(bandolier, inside - take);
            loaded += take;
        }
        if (loaded < needed && !player.getAbilities().instabuild) {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize() && loaded < needed; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(ModItems.MAGIC_CARTRIDGE.get())) {
                    int take = Math.min(stack.getCount(), needed - loaded);
                    stack.shrink(take);
                    loaded += take;
                }
            }
        } else if (player.getAbilities().instabuild) {
            loaded = needed;
        }
        if (loaded <= 0) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.gun.no_ammo").withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.6f);
            return;
        }
        gun.set(ModComponents.GUN_ROUNDS, rounds + loaded);
        player.getCooldowns().addCooldown(this, reloadTicks);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("arcane_artifacts.gun.rounds", rounds(stack), magazine).withStyle(ChatFormatting.GOLD));
        Spell spell = storedSpell(stack);
        tooltip.add(spell.isEmpty()
                ? Component.translatable("arcane_artifacts.gun.empty_glyph").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("arcane_artifacts.caster.stored", spell.getDisplayString()).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return rounds(stack) < magazine;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * rounds(stack) / magazine);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF8C1A;
    }
}
