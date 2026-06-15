package com.otectus.spells_n_gods.worldstate;

import com.otectus.spells_n_gods.spawning.logic.CooldownState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent per-structure-instance encounter record for the tag/tier spawning system. Tracks
 * cooldown/exhaustion, the running spawn budget (active deity UUIDs), and total spawns. The
 * pure cooldown/budget arithmetic is delegated to {@link CooldownState} so it stays testable.
 *
 * <p>The record is keyed by a stable identity (dimension + structure id + origin chunk) so the
 * same structure instance is recognised across relogs and restarts.
 */
public class EncounterRecord {
    private final String key;
    private final String structureId;
    private final String dimension;
    private final BoundingBox bounds;

    private long lastSpawnGameTime = -1L;
    private long cooldownEndsGameTime = 0L;
    private int spawnedCount = 0;
    private final Set<UUID> activeDeities = new LinkedHashSet<>();

    public EncounterRecord(String key, String structureId, String dimension, BoundingBox bounds) {
        this.key = key;
        this.structureId = structureId;
        this.dimension = dimension;
        this.bounds = bounds;
    }

    public String key() {
        return key;
    }

    public String structureId() {
        return structureId;
    }

    public String dimension() {
        return dimension;
    }

    public BoundingBox bounds() {
        return bounds;
    }

    public int activeDeityCount() {
        return activeDeities.size();
    }

    public int spawnedCount() {
        return spawnedCount;
    }

    public Set<UUID> activeDeities() {
        return java.util.Collections.unmodifiableSet(activeDeities);
    }

    public CooldownState toCooldownState() {
        return new CooldownState(lastSpawnGameTime, cooldownEndsGameTime, spawnedCount, activeDeities.size());
    }

    public boolean canSpawn(long now, int maxActiveDeities) {
        return toCooldownState().canSpawn(now, maxActiveDeities);
    }

    public boolean isPermanentlyExhausted() {
        return cooldownEndsGameTime == CooldownState.PERMANENT;
    }

    public boolean isPrunable(long now) {
        return toCooldownState().isPrunable(now);
    }

    /** Record a spawn: consume a budget slot, arm cooldown, and remember the deity's UUID. */
    public void recordSpawn(UUID deityUuid, long now, int cooldownDays) {
        CooldownState next = toCooldownState().withSpawn(now, cooldownDays);
        this.lastSpawnGameTime = next.lastSpawnGameTime();
        this.cooldownEndsGameTime = next.cooldownEndsGameTime();
        this.spawnedCount = next.spawnedCount();
        if (deityUuid != null) {
            this.activeDeities.add(deityUuid);
        }
    }

    /** Release a budget slot when an active deity dies/despawns. */
    public boolean releaseDeity(UUID deityUuid) {
        return activeDeities.remove(deityUuid);
    }

    public boolean hasActiveDeity(UUID deityUuid) {
        return activeDeities.contains(deityUuid);
    }

    // --- NBT ---

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Key", key);
        tag.putString("StructureId", structureId);
        tag.putString("Dimension", dimension);
        tag.putInt("MinX", bounds.minX());
        tag.putInt("MinY", bounds.minY());
        tag.putInt("MinZ", bounds.minZ());
        tag.putInt("MaxX", bounds.maxX());
        tag.putInt("MaxY", bounds.maxY());
        tag.putInt("MaxZ", bounds.maxZ());
        tag.putLong("LastSpawn", lastSpawnGameTime);
        tag.putLong("CooldownEnds", cooldownEndsGameTime);
        tag.putInt("SpawnedCount", spawnedCount);
        ListTag list = new ListTag();
        for (UUID uuid : activeDeities) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", uuid);
            list.add(entry);
        }
        tag.put("Active", list);
        return tag;
    }

    public static EncounterRecord fromTag(CompoundTag tag) {
        BoundingBox bounds = new BoundingBox(
                tag.getInt("MinX"), tag.getInt("MinY"), tag.getInt("MinZ"),
                tag.getInt("MaxX"), tag.getInt("MaxY"), tag.getInt("MaxZ"));
        EncounterRecord record = new EncounterRecord(
                tag.getString("Key"), tag.getString("StructureId"), tag.getString("Dimension"), bounds);
        record.lastSpawnGameTime = tag.getLong("LastSpawn");
        record.cooldownEndsGameTime = tag.getLong("CooldownEnds");
        record.spawnedCount = tag.getInt("SpawnedCount");
        ListTag list = tag.getList("Active", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag entry = (CompoundTag) t;
            if (entry.hasUUID("UUID")) {
                record.activeDeities.add(entry.getUUID("UUID"));
            }
        }
        return record;
    }
}
