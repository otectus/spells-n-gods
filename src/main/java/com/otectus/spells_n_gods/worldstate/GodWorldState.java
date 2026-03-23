package com.otectus.spells_n_gods.worldstate;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class GodWorldState extends SavedData {
    private static final String DATA_NAME = "sng_world_state";
    private final Map<String, StructureRecord> structures = new HashMap<>();

    public GodWorldState() {
    }

    private GodWorldState(CompoundTag tag) {
        CompoundTag structuresTag = tag.getCompound("Structures");
        for (String key : structuresTag.getAllKeys()) {
            CompoundTag recordTag = structuresTag.getCompound(key);
            structures.put(key, StructureRecord.fromTag(key, recordTag));
        }
    }

    public static GodWorldState get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                GodWorldState::new,
                GodWorldState::new,
                DATA_NAME
        );
    }

    // --- Query Methods ---

    public boolean isGodStructurePlaced(String godId) {
        return structures.containsKey(godId);
    }

    public Optional<StructureRecord> getStructure(String godId) {
        return Optional.ofNullable(structures.get(godId));
    }

    public Collection<StructureRecord> getAllStructures() {
        return Collections.unmodifiableCollection(structures.values());
    }

    public List<StructureRecord> getProtectedStructures() {
        return List.copyOf(structures.values());
    }

    /**
     * Find the structure for a specific god in the given dimension.
     * Since there is exactly one temple per god, this is a direct map lookup.
     */
    public Optional<StructureRecord> findStructureForGod(String godId, String dimension) {
        StructureRecord record = structures.get(godId);
        if (record != null && record.dimension().equals(dimension)) {
            return Optional.of(record);
        }
        return Optional.empty();
    }

    public Optional<StructureRecord> findNearestStructure(String dimension, BlockPos from) {
        StructureRecord nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (StructureRecord record : structures.values()) {
            if (!record.dimension().equals(dimension)) continue;
            double distSq = record.center().distSqr(from);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = record;
            }
        }
        return Optional.ofNullable(nearest);
    }

    // --- Mutation Methods ---

    public void setStructure(String godId, StructureRecord record) {
        structures.put(godId, record);
        setDirty();
    }

    public void registerStructure(String godId, BlockPos center, BoundingBox bounds, String dimension) {
        if (structures.containsKey(godId)) {
            SpellsNGodsMod.LOGGER.warn("Attempted to register duplicate structure for god {}", godId);
            return;
        }
        StructureRecord record = new StructureRecord(
                godId, center, bounds, dimension,
                0L, 0L, false, null, false
        );
        structures.put(godId, record);
        setDirty();
        SpellsNGodsMod.LOGGER.info("Registered structure for god {} at {} in {}", godId, center, dimension);
    }

    public void markBossKilled(String godId, long gameTime, int respawnDelayTicks) {
        StructureRecord old = structures.get(godId);
        if (old == null) {
            SpellsNGodsMod.LOGGER.warn("Cannot mark boss killed for unknown god {}", godId);
            return;
        }
        structures.put(godId, old.withBossKilled(gameTime, respawnDelayTicks));
        setDirty();
    }

    public void markBossSpawned(String godId, UUID entityUUID) {
        StructureRecord old = structures.get(godId);
        if (old == null) {
            SpellsNGodsMod.LOGGER.warn("Cannot mark boss spawned for unknown god {}", godId);
            return;
        }
        structures.put(godId, old.withBossSpawned(entityUUID));
        setDirty();
    }

    public void markBossAlive(String godId, boolean alive) {
        StructureRecord old = structures.get(godId);
        if (old == null) return;
        structures.put(godId, old.withBossAlive(alive));
        setDirty();
    }

    public boolean isPositionProtected(BlockPos pos) {
        for (StructureRecord record : structures.values()) {
            if (record.bounds().isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    // --- SavedData Contract ---

    @Override
    public CompoundTag save(CompoundTag root) {
        CompoundTag structuresTag = new CompoundTag();
        for (var entry : structures.entrySet()) {
            structuresTag.put(entry.getKey(), entry.getValue().toTag());
        }
        root.put("Structures", structuresTag);
        return root;
    }
}
