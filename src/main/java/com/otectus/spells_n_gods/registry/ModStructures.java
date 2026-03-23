package com.otectus.spells_n_gods.registry;

import com.mojang.serialization.Codec;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.structure.GodTempleStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, SpellsNGodsMod.MODID);

    public static final RegistryObject<StructureType<GodTempleStructure>> GOD_TEMPLE =
            STRUCTURE_TYPES.register("god_temple", () -> () -> GodTempleStructure.CODEC);

    private ModStructures() {}
}
