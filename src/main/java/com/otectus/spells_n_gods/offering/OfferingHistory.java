package com.otectus.spells_n_gods.offering;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.Deque;

public class OfferingHistory {
    private static final int MAX_ENTRIES = 50;
    private static final long DEFAULT_WINDOW_MS = 24 * 3_600_000L; // 24 hours

    private final Deque<OfferingEntry> entries = new ArrayDeque<>();

    public record OfferingEntry(String itemId, long epochMs) {
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("ItemId", itemId);
            tag.putLong("EpochMs", epochMs);
            return tag;
        }

        public static OfferingEntry load(CompoundTag tag) {
            return new OfferingEntry(
                    tag.getString("ItemId"),
                    tag.getLong("EpochMs")
            );
        }
    }

    public void record(Item item) {
        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        entries.addLast(new OfferingEntry(itemId, System.currentTimeMillis()));

        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    public int countRecent(Item item, long withinMs) {
        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        long cutoff = System.currentTimeMillis() - withinMs;
        return (int) entries.stream()
                .filter(e -> e.itemId().equals(itemId) && e.epochMs() > cutoff)
                .count();
    }

    public int countRecent(Item item) {
        return countRecent(item, DEFAULT_WINDOW_MS);
    }

    public float computeDiminishingMultiplier(Item item) {
        int recentCount = countRecent(item);
        if (recentCount == 0) {
            return 1.0f;
        }
        // Each repeat reduces value by 25%
        return (float) Math.pow(0.75, recentCount);
    }

    public void pruneOld(long olderThanMs) {
        long cutoff = System.currentTimeMillis() - olderThanMs;
        entries.removeIf(e -> e.epochMs() < cutoff);
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (OfferingEntry entry : entries) {
            list.add(entry.save());
        }
        tag.put("Entries", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        entries.clear();
        if (tag.contains("Entries", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                entries.addLast(OfferingEntry.load(list.getCompound(i)));
            }
        }
    }

    public static OfferingHistory fromTag(CompoundTag tag) {
        OfferingHistory history = new OfferingHistory();
        history.load(tag);
        return history;
    }
}
