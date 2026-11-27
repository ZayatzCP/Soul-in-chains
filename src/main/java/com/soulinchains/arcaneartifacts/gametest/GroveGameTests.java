package com.soulinchains.arcaneartifacts.gametest;

import com.mojang.authlib.GameProfile;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.grove.LightColor;
import com.soulinchains.arcaneartifacts.grove.LightJarBlock;
import com.soulinchains.arcaneartifacts.grove.LightJarBlockEntity;
import com.soulinchains.arcaneartifacts.grove.MansionLights;
import com.soulinchains.arcaneartifacts.grove.ThicketLight;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** The Thicket Lights (1.44): friends, stings, the light they carry, jars, and the illagers' cages. */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class GroveGameTests {
    private static final String ARENA = "arena";

    private GroveGameTests() {
    }

    private static ThicketLight light(GameTestHelper helper, BlockPos at, LightColor color) {
        ThicketLight light = helper.spawn(ModEntities.THICKET_LIGHT.get(), at);
        light.setColor(color);
        return light;
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void aFriendlyLightStingsAndShines(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "lightkeeper"));
        BlockPos feet = helper.absolutePos(new BlockPos(2, 2, 2));
        player.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        ThicketLight light = light(helper, new BlockPos(3, 3, 3), LightColor.MOSS);
        light.tame(player);
        helper.assertTrue(light.isTame() && light.isOwnedBy(player), "Tamed");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 2, 6));
        float before = zombie.getHealth();
        light.sting(level, player, zombie);
        helper.assertTrue(zombie.getHealth() < before, "The spark stings: " + before + " -> " + zombie.getHealth());
        zombie.setTarget(player);
        helper.assertTrue(light.enemyOf(player) == zombie, "It sees who hunts its friend");
        light.setOrderedToSit(true);
        helper.runAfterDelay(12, () -> {
            BlockPos lit = light.litAt();
            helper.assertTrue(lit != null && level.getBlockState(lit).is(Blocks.LIGHT), "A tame light lights the air around it");
            light.discard();
            helper.assertTrue(!level.getBlockState(lit).is(Blocks.LIGHT), "Its light goes with it");
            zombie.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void aLightGoesIntoAJarAndComesOut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ThicketLight light = light(helper, new BlockPos(3, 3, 3), LightColor.FROST);
        light.setCustomName(net.minecraft.network.chat.Component.literal("Pip's"));
        ItemStack jar = light.jar();
        helper.assertTrue(jar.has(DataComponents.BLOCK_ENTITY_DATA) && jar.has(DataComponents.BLOCK_STATE), "The jar keeps the light and its colour");
        light.discard();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        level.setBlock(pos, ModBlocks.LIGHT_JAR.get().defaultBlockState().setValue(LightJarBlock.COLOR, LightColor.FROST), 3);
        LightJarBlockEntity be = (LightJarBlockEntity) level.getBlockEntity(pos);
        be.setLight(jar.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getCompound(LightJarBlockEntity.LIGHT));
        helper.assertTrue(ModBlocks.LIGHT_JAR.get().defaultBlockState().getLightEmission() >= 14, "A jar is a lamp");
        ThicketLight out = LightJarBlock.open(level, pos, level.getBlockState(pos));
        helper.assertTrue(out != null && out.color() == LightColor.FROST && out.hasCustomName() && "Pip's".equals(out.getCustomName().getString()),
                "The same light comes out");
        helper.assertTrue(level.getBlockState(pos).isAir(), "The jar is gone");
        out.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aBrokenCageFreesAWildLight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        level.setBlock(pos, ModBlocks.LIGHT_CAGE.get().defaultBlockState().setValue(LightJarBlock.COLOR, LightColor.ROSE), 3);
        level.destroyBlock(pos, true);
        List<ThicketLight> freed = level.getEntitiesOfClass(ThicketLight.class, new AABB(pos).inflate(2));
        helper.assertTrue(freed.size() == 1 && freed.get(0).color() == LightColor.ROSE, "The caged light flies out");
        helper.assertTrue(!freed.get(0).isTame(), "Free, but nobody's friend");
        freed.forEach(ThicketLight::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mansionIllagersKeepCagedLights(GameTestHelper helper) throws ClassNotFoundException {
        // loading the mansion piece applies the mixin; a broken injection fails here, not in someone's world
        Class.forName("net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces$WoodlandMansionPiece");
        ServerLevel level = helper.getLevel();
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.DARK_OAK_PLANKS);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }
        BlockPos marker = helper.absolutePos(new BlockPos(4, 2, 4));
        int placed = 0;
        for (int i = 0; i < 40; i++) {
            BlockPos at = MansionLights.onMarker("Warrior", marker, level, level.random, null);
            if (at != null) {
                helper.assertTrue(level.getBlockState(at).is(ModBlocks.LIGHT_CAGE.get()) && at.distManhattan(marker) <= 2, "A cage beside the illager");
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 2 | 16);
                placed++;
            }
        }
        helper.assertTrue(placed > 4 && placed < 30, "About two illagers in five keep a light, got " + placed + " of 40");
        helper.assertTrue(MansionLights.onMarker("Chest", marker, level, level.random, null) == null, "Chests keep no lights");
        level.getEntitiesOfClass(ThicketLight.class, new AABB(marker).inflate(4)).forEach(ThicketLight::discard);
        helper.succeed();
    }
}
