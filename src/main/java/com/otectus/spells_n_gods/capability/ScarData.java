package com.otectus.spells_n_gods.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks permanent scars from apostasy.
 * Each apostasy adds a scar with cumulative penalties.
 */
public class ScarData {
    private final List<ScarRecord> scars = new ArrayList<>();
    private long lastApostasyEpochMs;

    /**
     * Record of a single apostasy scar.
     */
    public record ScarRecord(
            String sourceGodId,
            long timestamp,
            float healthReduction,
            float xpPenalty,
            float deathPenaltyIncrease,
            float luckReduction,
            float durabilityPenalty
    ) {
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("SourceGod", sourceGodId);
            tag.putLong("Timestamp", timestamp);
            tag.putFloat("HealthReduction", healthReduction);
            tag.putFloat("XpPenalty", xpPenalty);
            tag.putFloat("DeathPenalty", deathPenaltyIncrease);
            tag.putFloat("LuckReduction", luckReduction);
            tag.putFloat("DurabilityPenalty", durabilityPenalty);
            return tag;
        }

        public static ScarRecord load(CompoundTag tag) {
            return new ScarRecord(
                    tag.getString("SourceGod"),
                    tag.getLong("Timestamp"),
                    tag.getFloat("HealthReduction"),
                    tag.getFloat("XpPenalty"),
                    tag.getFloat("DeathPenalty"),
                    tag.getFloat("LuckReduction"),
                    tag.getFloat("DurabilityPenalty")
            );
        }
    }

    public void addScar(ScarRecord scar) {
        scars.add(scar);
        lastApostasyEpochMs = scar.timestamp();
    }

    public void clear() {
        scars.clear();
        lastApostasyEpochMs = 0L;
    }

    public List<ScarRecord> getScars() {
        return List.copyOf(scars);
    }

    public int getScarCount() {
        return scars.size();
    }

    public long getLastApostasyEpochMs() {
        return lastApostasyEpochMs;
    }

    /**
     * Calculate total health reduction from all scars.
     */
    public float getTotalHealthReduction() {
        return (float) scars.stream()
                .mapToDouble(ScarRecord::healthReduction)
                .sum();
    }

    /**
     * Calculate total XP penalty from all scars.
     */
    public float getTotalXpPenalty() {
        return (float) scars.stream()
                .mapToDouble(ScarRecord::xpPenalty)
                .sum();
    }

    /**
     * Calculate total death penalty increase from all scars.
     */
    public float getTotalDeathPenalty() {
        return (float) scars.stream()
                .mapToDouble(ScarRecord::deathPenaltyIncrease)
                .sum();
    }

    /**
     * Calculate total luck reduction from all scars.
     */
    public float getTotalLuckReduction() {
        return (float) scars.stream()
                .mapToDouble(ScarRecord::luckReduction)
                .sum();
    }

    /**
     * Calculate total durability penalty from all scars.
     */
    public float getTotalDurabilityPenalty() {
        return (float) scars.stream()
                .mapToDouble(ScarRecord::durabilityPenalty)
                .sum();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("LastApostasy", lastApostasyEpochMs);

        ListTag scarList = new ListTag();
        for (ScarRecord scar : scars) {
            scarList.add(scar.save());
        }
        tag.put("Scars", scarList);

        return tag;
    }

    public void load(CompoundTag tag) {
        lastApostasyEpochMs = tag.getLong("LastApostasy");
        scars.clear();

        ListTag scarList = tag.getList("Scars", Tag.TAG_COMPOUND);
        for (int i = 0; i < scarList.size(); i++) {
            scars.add(ScarRecord.load(scarList.getCompound(i)));
        }
    }

    public void copyFrom(ScarData other) {
        this.scars.clear();
        this.scars.addAll(other.scars);
        this.lastApostasyEpochMs = other.lastApostasyEpochMs;
    }
}
