package com.soulinchains.arcaneartifacts.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the village pools take Catto's stalls, storerooms and junk piles (see CattoVillage). */
@Mixin(StructureTemplatePool.class)
public interface StructureTemplatePoolAccessor {
    @Accessor("templates")
    ObjectArrayList<StructurePoolElement> arcaneArtifacts$templates();
}
