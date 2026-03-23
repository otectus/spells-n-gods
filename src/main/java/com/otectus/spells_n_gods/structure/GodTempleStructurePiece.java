package com.otectus.spells_n_gods.structure;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.config.ShrineConfig;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Map;
import java.util.Optional;

public class GodTempleStructurePiece extends StructurePiece {

    public static final StructurePieceType TYPE = (ctx, tag) -> new GodTempleStructurePiece(tag);

    private static final int HALF_SIZE = 12;
    private static final int WALL_HEIGHT = 3;
    private static final int PILLAR_HEIGHT = 5;

    private final String godId;

    // Block themes per god magic school
    private static final Map<String, BlockState[]> THEME_BLOCKS = Map.of(
            "holy",      new BlockState[]{ Blocks.QUARTZ_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.GLOWSTONE.defaultBlockState() },
            "lightning",  new BlockState[]{ Blocks.COPPER_BLOCK.defaultBlockState(), Blocks.OXIDIZED_COPPER.defaultBlockState(), Blocks.LIGHTNING_ROD.defaultBlockState() },
            "blood",     new BlockState[]{ Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Blocks.RED_NETHER_BRICKS.defaultBlockState() },
            "fire",      new BlockState[]{ Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState() },
            "evocation", new BlockState[]{ Blocks.PRISMARINE_BRICKS.defaultBlockState(), Blocks.END_STONE_BRICKS.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState() },
            "ender",     new BlockState[]{ Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.END_STONE_BRICKS.defaultBlockState(), Blocks.END_ROD.defaultBlockState() },
            "nature",    new BlockState[]{ Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), Blocks.GLOWSTONE.defaultBlockState() },
            "eldritch",  new BlockState[]{ Blocks.CRYING_OBSIDIAN.defaultBlockState(), Blocks.AMETHYST_BLOCK.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState() },
            "ice",       new BlockState[]{ Blocks.PACKED_ICE.defaultBlockState(), Blocks.BLUE_ICE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState() }
    );

    public GodTempleStructurePiece(BlockPos center, String godId) {
        super(TYPE, 0, new BoundingBox(
                center.getX() - HALF_SIZE, center.getY() - 1, center.getZ() - HALF_SIZE,
                center.getX() + HALF_SIZE, center.getY() + PILLAR_HEIGHT + 1, center.getZ() + HALF_SIZE
        ));
        this.godId = godId;
        this.setOrientation(null);
    }

    public GodTempleStructurePiece(CompoundTag tag) {
        super(TYPE, tag);
        this.godId = tag.getString("GodId");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("GodId", godId);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBounds, ChunkPos chunkPos, BlockPos pos) {

        int cx = (boundingBox.minX() + boundingBox.maxX()) / 2;
        int cy = boundingBox.minY() + 1;
        int cz = (boundingBox.minZ() + boundingBox.maxZ()) / 2;

        // Try custom schematic first
        boolean placedSchematic = false;
        if (ShrineConfig.INSTANCE.useCustomSchematics.get()) {
            Optional<StructureTemplate> schematic = SchematicLoader.getSchematic(godId);
            if (schematic.isPresent()) {
                placedSchematic = placeSchematic(level, schematic.get(), cx, cy, cz, random);
            }
        }

        // Fall back to procedural generation
        if (!placedSchematic) {
            if (ShrineConfig.INSTANCE.useCustomSchematics.get()
                    && !ShrineConfig.INSTANCE.fallbackToProcedural.get()) {
                // Schematics enabled but no schematic found and fallback disabled — skip generation
                SpellsNGodsMod.LOGGER.debug("No schematic for {} and fallback disabled — skipping structure", godId);
            } else {
                generateProcedural(level, chunkBounds, cx, cy, cz);
            }
        }

        // Register with GodWorldState so RespawnTickHandler will spawn the boss.
        // Only register once: when the chunk containing the center is being processed.
        BlockPos center = new BlockPos(cx, cy, cz);
        if (chunkBounds.isInside(center)) {
            try {
                ServerLevel serverLevel = level.getLevel();
                if (serverLevel != null && serverLevel.getServer() != null) {
                    GodWorldState state = GodWorldState.get(serverLevel.getServer());
                    if (!state.isGodStructurePlaced(godId)) {
                        String dimension = serverLevel.dimension().location().toString();
                        state.registerStructure(godId, center, this.boundingBox, dimension);
                        // Set respawnDueTime to 1 so the boss spawns on the next tick check
                        state.markBossKilled(godId, 0L, 1);
                        SpellsNGodsMod.LOGGER.info("God temple for {} registered at {} — boss will spawn shortly", godId, center);
                    }
                }
            } catch (Exception e) {
                SpellsNGodsMod.LOGGER.warn("Failed to register god temple for {}: {}", godId, e.getMessage());
            }
        }
    }

    /**
     * Places a custom .nbt schematic centered at the given position.
     * The schematic is centered on (cx, cy, cz) based on its size.
     */
    private boolean placeSchematic(WorldGenLevel level, StructureTemplate template,
                                    int cx, int cy, int cz, RandomSource random) {
        try {
            var size = template.getSize();
            // Center the schematic on cx/cz, place at cy (floor level)
            BlockPos origin = new BlockPos(
                    cx - size.getX() / 2,
                    cy,
                    cz - size.getZ() / 2
            );

            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setIgnoreEntities(false);

            template.placeInWorld(level, origin, origin, settings, random, 2);
            SpellsNGodsMod.LOGGER.info("Placed custom schematic for {} at {}", godId, origin);
            return true;
        } catch (Exception e) {
            SpellsNGodsMod.LOGGER.error("Failed to place schematic for {}: {}", godId, e.getMessage());
            return false;
        }
    }

    /**
     * Original procedural structure generation.
     */
    private void generateProcedural(WorldGenLevel level, BoundingBox chunkBounds,
                                     int cx, int cy, int cz) {
        String school = resolveSchool();
        BlockState[] theme = THEME_BLOCKS.getOrDefault(school, THEME_BLOCKS.get("holy"));
        BlockState floor = theme[0];
        BlockState accent = theme[1];
        BlockState light = theme[2];

        // Floor platform (25x25)
        for (int dx = -HALF_SIZE; dx <= HALF_SIZE; dx++) {
            for (int dz = -HALF_SIZE; dz <= HALF_SIZE; dz++) {
                BlockPos floorPos = new BlockPos(cx + dx, cy, cz + dz);
                if (chunkBounds.isInside(floorPos)) {
                    if ((Math.abs(dx) + Math.abs(dz)) % 7 == 0) {
                        level.setBlock(floorPos, accent, 2);
                    } else {
                        level.setBlock(floorPos, floor, 2);
                    }
                }
            }
        }

        // Clear air above floor
        for (int dy = 1; dy <= PILLAR_HEIGHT + 1; dy++) {
            for (int dx = -HALF_SIZE; dx <= HALF_SIZE; dx++) {
                for (int dz = -HALF_SIZE; dz <= HALF_SIZE; dz++) {
                    BlockPos airPos = new BlockPos(cx + dx, cy + dy, cz + dz);
                    if (chunkBounds.isInside(airPos)) {
                        level.setBlock(airPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Low walls (3 blocks tall) around perimeter
        for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
            for (int dx = -HALF_SIZE; dx <= HALF_SIZE; dx++) {
                setBlockIfInBounds(level, chunkBounds, new BlockPos(cx + dx, cy + dy, cz - HALF_SIZE), floor);
                setBlockIfInBounds(level, chunkBounds, new BlockPos(cx + dx, cy + dy, cz + HALF_SIZE), floor);
            }
            for (int dz = -HALF_SIZE + 1; dz < HALF_SIZE; dz++) {
                setBlockIfInBounds(level, chunkBounds, new BlockPos(cx - HALF_SIZE, cy + dy, cz + dz), floor);
                setBlockIfInBounds(level, chunkBounds, new BlockPos(cx + HALF_SIZE, cy + dy, cz + dz), floor);
            }
        }

        // 4 corner pillars (5 blocks tall)
        int[][] corners = {{-HALF_SIZE, -HALF_SIZE}, {-HALF_SIZE, HALF_SIZE}, {HALF_SIZE, -HALF_SIZE}, {HALF_SIZE, HALF_SIZE}};
        for (int[] corner : corners) {
            for (int dy = 1; dy <= PILLAR_HEIGHT; dy++) {
                BlockPos pillarPos = new BlockPos(cx + corner[0], cy + dy, cz + corner[1]);
                setBlockIfInBounds(level, chunkBounds, pillarPos, accent);
            }
            BlockPos lightPos = new BlockPos(cx + corner[0], cy + PILLAR_HEIGHT + 1, cz + corner[1]);
            setBlockIfInBounds(level, chunkBounds, lightPos, light);
        }

        // Central altar (3x3 raised platform with light)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setBlockIfInBounds(level, chunkBounds, new BlockPos(cx + dx, cy + 1, cz + dz), accent);
            }
        }
        setBlockIfInBounds(level, chunkBounds, new BlockPos(cx, cy + 2, cz), light);
    }

    private void setBlockIfInBounds(WorldGenLevel level, BoundingBox bounds, BlockPos pos, BlockState state) {
        if (bounds.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    private String resolveSchool() {
        // Map god IDs to schools. This is a fallback; ideally read from GodDefinition at runtime.
        return switch (godId.replace("spells_n_gods:", "")) {
            case "deus" -> "holy";
            case "velox" -> "lightning";
            case "celia" -> "blood";
            case "bella" -> "fire";
            case "bricoleur" -> "evocation";
            case "ingenium" -> "ender";
            case "venatas" -> "nature";
            case "magnus" -> "eldritch";
            case "glacia" -> "ice";
            default -> "holy";
        };
    }
}
