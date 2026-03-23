package com.otectus.spells_n_gods.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.spells_n_gods.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

public class GodTempleStructure extends Structure {

    public static final Codec<GodTempleStructure> CODEC = RecordCodecBuilder.<GodTempleStructure>mapCodec(
            instance -> instance.group(
                    settingsCodec(instance),
                    Codec.STRING.fieldOf("god_id").forGetter(s -> s.godId)
            ).apply(instance, GodTempleStructure::new)
    ).codec();

    private final String godId;

    public GodTempleStructure(StructureSettings settings, String godId) {
        super(settings);
        this.godId = godId;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // Get terrain height at the chunk center
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();

        // Use WORLD_SURFACE heightmap to find the surface
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());

        BlockPos centerPos = new BlockPos(x, y, z);

        return Optional.of(new GenerationStub(centerPos, (builder) -> {
            generatePieces(builder, context, centerPos);
        }));
    }

    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos center) {
        builder.addPiece(new GodTempleStructurePiece(center, godId));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.GOD_TEMPLE.get();
    }
}
