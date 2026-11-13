package com.soulinchains.arcaneartifacts.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Furniture of the Mycelinoid houses: a mushroom table, a stool and a hanging lamp. */
public class MushroomFurnitureBlock extends Block {
    public enum Kind {
        TABLE(Shapes.or(Block.box(0, 13, 0, 16, 16, 16), Block.box(5, 0, 5, 11, 13, 11))),
        STOOL(Shapes.or(Block.box(2, 6, 2, 14, 9, 14), Block.box(5, 0, 5, 11, 6, 11))),
        LAMP(Shapes.or(Block.box(3, 4, 3, 13, 9, 13), Block.box(6, 9, 6, 10, 16, 10)));

        final VoxelShape shape;

        Kind(VoxelShape shape) {
            this.shape = shape;
        }
    }

    private final Kind kind;

    public MushroomFurnitureBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(properties -> new MushroomFurnitureBlock(kind, properties));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.shape;
    }
}
