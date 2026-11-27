package com.soulinchains.arcaneartifacts.registry;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, ArcaneArtifacts.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneMissileEntity>> ARCANE_MISSILE = REGISTER.register("arcane_missile",
            () -> EntityType.Builder.<ArcaneMissileEntity>of(ArcaneMissileEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArcaneArtifacts.MODID + ":arcane_missile"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.SoulWisp>> SOUL_WISP = REGISTER.register("soul_wisp",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.necro.SoulWisp>of(com.soulinchains.arcaneartifacts.necro.SoulWisp::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).noSave().fireImmune().clientTrackingRange(8).updateInterval(2)
                    .build(ArcaneArtifacts.MODID + ":soul_wisp"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.ChainBolt>> CHAIN_BOLT = REGISTER.register("chain_bolt",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.necro.ChainBolt>of(com.soulinchains.arcaneartifacts.necro.ChainBolt::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).noSave().fireImmune().clientTrackingRange(8).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build(ArcaneArtifacts.MODID + ":chain_bolt"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.ShackleAnchor>> SHACKLE_ANCHOR = REGISTER.register("shackle_anchor",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.necro.ShackleAnchor>of(com.soulinchains.arcaneartifacts.necro.ShackleAnchor::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).noSave().fireImmune().clientTrackingRange(8).updateInterval(10)
                    .build(ArcaneArtifacts.MODID + ":shackle_anchor"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.BondLink>> BOND_LINK = REGISTER.register("bond_link",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.necro.BondLink>of(com.soulinchains.arcaneartifacts.necro.BondLink::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).noSave().fireImmune().clientTrackingRange(8).updateInterval(2)
                    .build(ArcaneArtifacts.MODID + ":bond_link"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.mob.Restless>> RESTLESS = REGISTER.register("restless",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.necro.mob.Restless::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":restless"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.mob.SoulEater>> SOUL_EATER = REGISTER.register("soul_eater",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.necro.mob.SoulEater::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.9F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":soul_eater"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.mob.Mourner>> MOURNER = REGISTER.register("mourner",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.necro.mob.Mourner::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":mourner"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.mob.ChainGhost>> CHAIN_GHOST = REGISTER.register("chain_ghost",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.necro.mob.ChainGhost::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).fireImmune().clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":chain_ghost"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.mob.Jailer>> JAILER = REGISTER.register("jailer",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.necro.mob.Jailer::new, MobCategory.MONSTER)
                    .sized(0.9F, 3.0F).fireImmune().clientTrackingRange(12).build(ArcaneArtifacts.MODID + ":jailer"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.mob.ImprisonedKing>> IMPRISONED_KING = REGISTER.register("imprisoned_king",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.necro.mob.ImprisonedKing::new, MobCategory.MONSTER)
                    .sized(1.2F, 4.0F).fireImmune().clientTrackingRange(16).build(ArcaneArtifacts.MODID + ":imprisoned_king"));




    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.rite.Echo>> ECHO = REGISTER.register("echo",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.rite.Echo::new, MobCategory.MONSTER).sized(0.6F, 1.9F).clientTrackingRange(10)
                    .build(ArcaneArtifacts.MODID + ":echo"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.grove.ThicketLight>> THICKET_LIGHT = REGISTER.register("thicket_light",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.grove.ThicketLight::new, MobCategory.AMBIENT).sized(0.35F, 0.35F).clientTrackingRange(10)
                    .build(ArcaneArtifacts.MODID + ":thicket_light"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.astral.Watcher>> WATCHER = REGISTER.register("watcher",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.astral.Watcher::new, MobCategory.MISC).sized(1.5F, 7.2F).clientTrackingRange(16)
                    .noSave().build(ArcaneArtifacts.MODID + ":watcher"));

    /** The Shackled's servants, one entity type per kind. */
    public static final java.util.Map<com.soulinchains.arcaneartifacts.necro.ServantKind,
            DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.Servant>>> SERVANTS = servants();

    private static java.util.Map<com.soulinchains.arcaneartifacts.necro.ServantKind,
            DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.Servant>>> servants() {
        var map = new java.util.EnumMap<com.soulinchains.arcaneartifacts.necro.ServantKind,
                DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.necro.Servant>>>(com.soulinchains.arcaneartifacts.necro.ServantKind.class);
        for (var kind : com.soulinchains.arcaneartifacts.necro.ServantKind.values()) {
            float width = kind == com.soulinchains.arcaneartifacts.necro.ServantKind.HOUND ? 0.6F : kind == com.soulinchains.arcaneartifacts.necro.ServantKind.KNIGHT ? 0.7F : 0.6F;
            float height = kind == com.soulinchains.arcaneartifacts.necro.ServantKind.HOUND ? 0.85F : kind == com.soulinchains.arcaneartifacts.necro.ServantKind.KNIGHT ? 2.2F : 1.95F;
            EntityType.Builder<com.soulinchains.arcaneartifacts.necro.Servant> builder = EntityType.Builder
                    .<com.soulinchains.arcaneartifacts.necro.Servant>of((type, level) -> new com.soulinchains.arcaneartifacts.necro.Servant(type, level, kind), MobCategory.MISC)
                    .sized(width, height).clientTrackingRange(10);
            if (kind.fireImmune) {
                builder.fireImmune();
            }
            map.put(kind, REGISTER.register(kind.id, () -> builder.build(ArcaneArtifacts.MODID + ":" + kind.id)));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.Mudzya>> MUDZYA = REGISTER.register("mudzya",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.Mudzya::new, MobCategory.CREATURE)
                    .sized(0.55F, 0.65F).clientTrackingRange(8).build(ArcaneArtifacts.MODID + ":mudzya"));
    // Mycelinoids are slim enough (0.6-0.65) to pass an open door: its leaf leaves about 0.8 of a block.
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.Gribushka>> GRIBUSHKA = REGISTER.register("gribushka",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.Gribushka::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.85F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":gribushka"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.Amanitoid>> AMANITOID = REGISTER.register("amanitoid",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.Amanitoid::new, MobCategory.CREATURE)
                    .sized(0.65F, 1.85F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":amanitoid"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.ElderMycelinoid>> ELDER_MYCELINOID = REGISTER.register("elder_mycelinoid",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.ElderMycelinoid::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.85F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":elder_mycelinoid"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.Gribulka>> GRIBULKA = REGISTER.register("gribulka",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.Gribulka::new, MobCategory.CREATURE)
                    .sized(0.45F, 0.6F).clientTrackingRange(8).build(ArcaneArtifacts.MODID + ":gribulka"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.entity.GribulkaHive>> GRIBULKA_HIVE = REGISTER.register("gribulka_hive",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.entity.GribulkaHive::new, MobCategory.CREATURE)
                    .sized(1.2F, 2.4F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":gribulka_hive"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.spore.SporeCloud>> SPORE_CLOUD = REGISTER.register("spore_cloud",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.spore.SporeCloud>of(com.soulinchains.arcaneartifacts.spore.SporeCloud::new, MobCategory.MISC)
                    .sized(6.0F, 0.5F).fireImmune().noSave().clientTrackingRange(10).updateInterval(Integer.MAX_VALUE)
                    .build(ArcaneArtifacts.MODID + ":spore_cloud"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.item.CrystalOrbEntity>> CRYSTAL_ORB = REGISTER.register("crystal_orb",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.item.CrystalOrbEntity>of(com.soulinchains.arcaneartifacts.item.CrystalOrbEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(8).updateInterval(10).build(ArcaneArtifacts.MODID + ":crystal_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.pseudo.BulletEntity>> BULLET = REGISTER.register("bullet",
            () -> EntityType.Builder.<com.soulinchains.arcaneartifacts.pseudo.BulletEntity>of(com.soulinchains.arcaneartifacts.pseudo.BulletEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).noSave().clientTrackingRange(8).updateInterval(1)
                    .build(ArcaneArtifacts.MODID + ":bullet"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.mount.FlyingBroom>> FLYING_BROOM = REGISTER.register("flying_broom",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.mount.FlyingBroom::new, MobCategory.MISC)
                    .sized(0.9F, 0.5F).passengerAttachments(0.3F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":flying_broom"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.mount.MossElk>> MOSS_ELK = REGISTER.register("moss_elk",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.mount.MossElk::new, MobCategory.MISC)
                    .sized(1.3F, 1.8F).passengerAttachments(1.55F).clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":moss_elk"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.soulinchains.arcaneartifacts.mount.IronHorse>> IRON_HORSE = REGISTER.register("iron_horse",
            () -> EntityType.Builder.of(com.soulinchains.arcaneartifacts.mount.IronHorse::new, MobCategory.MISC)
                    .sized(1.3F, 1.7F).passengerAttachments(1.5F).fireImmune().clientTrackingRange(10).build(ArcaneArtifacts.MODID + ":iron_horse"));

    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.soulinchains.arcaneartifacts.mount.IronHorseMenu>> IRON_HORSE_MENU =
            ModBlocks.MENUS.register("iron_horse", () -> new net.minecraft.world.inventory.MenuType<>(com.soulinchains.arcaneartifacts.mount.IronHorseMenu::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private ModEntities() {
    }
}
