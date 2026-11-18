package com.soulinchains.arcaneartifacts.catto;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Catto's magic weapons: any Ars Nouveau spell caster with a spell inscribed (an enchanter's sword, a wand, a spell
 * book...) casts its spell at Catto's foe. The spell is paid from Catto's own Source pool, which grows with the Alchemist
 * skill and with magic headwear and armor.
 */
public final class CattoCombat {
    private CattoCombat() {
    }

    @Nullable
    public static Spell spellOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster == null) {
            return null;
        }
        Spell spell = caster.getSpell();
        return spell == null || spell.isEmpty() ? null : spell;
    }

    public static int cost(Catto cat, Spell spell) {
        int cost = Math.max(0, spell.getCost());
        return cat.fullSet() == CattoGear.Set.ARCANE ? cost * 7 / 10 : cost;
    }

    /** Casts the weapon's spell at the target; false if there is no spell or not enough Source. */
    public static boolean cast(Catto cat, LivingEntity target) {
        Spell spell = spellOf(cat.weapon());
        if (spell == null) {
            return false;
        }
        int cost = cost(cat, spell);
        boolean free = cat.fullSet() == CattoGear.Set.PSEUDOMAGE && cat.getRandom().nextInt(5) == 0;
        if (!free && cat.source < cost) {
            return false;
        }
        cat.lookAt(target, 360.0F, 360.0F);
        SpellResolver resolver = new SpellResolver(new SpellContext(cat.level(), spell, cat, LivingCaster.from(cat))).withSilent(true);
        boolean cast = resolver.onCastOnEntity(cat.weapon(), target, InteractionHand.MAIN_HAND);
        if (cast) {
            if (!free) {
                cat.spendSource(cost);
            }
            cat.addXp(CattoSkill.ALCHEMIST, 1);
        }
        return cast;
    }

    /** Ticks between two blows: the Fighter strikes faster, and so does a Catto in a Pseudomage set. */
    public static int attackCooldown(Catto cat) {
        int ticks = switch (cat.tier(CattoSkill.FIGHTER)) {
            case 3 -> 12;
            case 2 -> 16;
            default -> 20;
        };
        return cat.fullSet() == CattoGear.Set.PSEUDOMAGE ? ticks * 3 / 4 : ticks;
    }

    /** Extra damage of the Fighter's tiers, added to Catto's base attack. */
    public static double fighterDamage(Catto cat) {
        return switch (cat.tier(CattoSkill.FIGHTER)) {
            case 3 -> 4.0;
            case 2 -> 2.0;
            default -> 0.0;
        };
    }
}
