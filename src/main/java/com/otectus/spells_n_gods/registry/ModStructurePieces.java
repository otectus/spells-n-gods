package com.otectus.spells_n_gods.registry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.structure.GodTempleStructurePiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructurePieces {
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, SpellsNGodsMod.MODID);

    public static final RegistryObject<StructurePieceType> GOD_TEMPLE_PIECE =
            STRUCTURE_PIECES.register("god_temple_piece", () -> GodTempleStructurePiece.TYPE);

    private ModStructurePieces() {}
}
