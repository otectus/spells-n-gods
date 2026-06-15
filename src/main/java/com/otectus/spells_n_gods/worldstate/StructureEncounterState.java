package com.otectus.spells_n_gods.worldstate;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-level saved data for the tag/tier deity spawning system. Stores one
 * {@link EncounterRecord} per detected structure instance (for cooldowns, exhaustion and spawn
 * budgets) plus a compact set of first-discovery flags for player-facing polish.
 *
 * <p>Persisted on the overworld {@link DimensionDataStorage}, separate from the legacy
 * {@link GodWorldState} so the two systems never interfere. Records are pruned automatically
 * once their cooldown elapses and no deities remain active, keeping the file bounded.
 */
public class StructureEncounterState extends SavedData {
    private static final String DATA_NAME = "sng_structure_encounters";

    private final Map<String, EncounterRecord> records = new LinkedHashMap<>();
    /** Compact "playerUuid|deityId|structureId" markers for first-discovery announcements. */
    private final Set<String> discoveries = new HashSet<>();

    public StructureEncounterState() {
    }

    private StructureEncounterState(CompoundTag tag) {
        ListTag recordList = tag.getList("Records", Tag.TAG_COMPOUND);
        for (Tag t : recordList) {
            EncounterRecord record = EncounterRecord.fromTag((CompoundTag) t);
            records.put(record.key(), record);
        }
        ListTag discoveryList = tag.getList("Discoveries", Tag.TAG_STRING);
        for (Tag t : discoveryList) {
            discoveries.add(t.getAsString());
        }
    }

    public static StructureEncounterState get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(StructureEncounterState::new, StructureEncounterState::new, DATA_NAME);
    }

    /** Build the stable key for a structure instance: dimension + id + origin chunk. */
    public static String makeKey(String dimension, String structureId, BoundingBox bounds) {
        int chunkX = bounds.minX() >> 4;
        int chunkZ = bounds.minZ() >> 4;
        return dimension + "@" + structureId + "@" + chunkX + ":" + chunkZ;
    }

    public EncounterRecord getOrCreate(String dimension, String structureId, BoundingBox bounds) {
        String key = makeKey(dimension, structureId, bounds);
        return records.computeIfAbsent(key, k -> {
            setDirty();
            return new EncounterRecord(k, structureId, dimension, bounds);
        });
    }

    public EncounterRecord get(String key) {
        return records.get(key);
    }

    public Collection<EncounterRecord> all() {
        return Collections.unmodifiableCollection(records.values());
    }

    public void recordSpawn(EncounterRecord record, UUID deityUuid, long now, int cooldownDays) {
        record.recordSpawn(deityUuid, now, cooldownDays);
        setDirty();
    }

    /**
     * Release a deity UUID from whatever record holds it (called when a tag-spawned deity dies).
     *
     * @return {@code true} if the UUID was found and released
     */
    public boolean releaseDeity(UUID deityUuid) {
        for (EncounterRecord record : records.values()) {
            if (record.releaseDeity(deityUuid)) {
                setDirty();
                return true;
            }
        }
        return false;
    }

    /**
     * Mark a first discovery. @return {@code true} if this is genuinely the first time
     * (so callers can announce); {@code false} if already discovered.
     */
    public boolean markDiscovered(UUID player, String deityId, String structureId) {
        String marker = player + "|" + deityId + "|" + structureId;
        boolean added = discoveries.add(marker);
        if (added) {
            setDirty();
        }
        return added;
    }

    /** Drop records that carry no active deities and whose (non-permanent) cooldown has elapsed. */
    public int prune(long now) {
        int removed = 0;
        Iterator<Map.Entry<String, EncounterRecord>> it = records.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isPrunable(now)) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            setDirty();
            SpellsNGodsMod.LOGGER.debug("[Sng/Spawn] Pruned {} expired structure encounter records", removed);
        }
        return removed;
    }

    public int size() {
        return records.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag recordList = new ListTag();
        for (EncounterRecord record : records.values()) {
            recordList.add(record.toTag());
        }
        tag.put("Records", recordList);
        ListTag discoveryList = new ListTag();
        List_addAll(discoveryList, discoveries);
        tag.put("Discoveries", discoveryList);
        return tag;
    }

    private static void List_addAll(ListTag list, Set<String> values) {
        List<String> sorted = new ArrayList<>(values);
        Collections.sort(sorted); // stable on-disk ordering
        for (String s : sorted) {
            list.add(StringTag.valueOf(s));
        }
    }
}
