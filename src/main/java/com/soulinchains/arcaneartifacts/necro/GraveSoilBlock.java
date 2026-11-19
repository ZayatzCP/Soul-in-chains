package com.soulinchains.arcaneartifacts.necro;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Graveyard soil. Consecrated: no hostile creature can spawn on it. Desecrated: servants raised on it come back 30%
 * stronger (health and damage).
 */
public class GraveSoilBlock extends Block {
    public static final double DESECRATED_BONUS = 0.3;
    public final boolean consecrated;

    public GraveSoilBlock(boolean consecrated) {
        super(BlockBehaviour.Properties.of().mapColor(consecrated ? MapColor.SAND : MapColor.COLOR_BLACK).strength(0.6F).sound(SoundType.SOUL_SOIL)
                .isValidSpawn((state, level, pos, type) -> !consecrated || type.getCategory() != MobCategory.MONSTER));
        this.consecrated = consecrated;
    }
}
