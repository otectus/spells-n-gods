package com.otectus.spells_n_gods.capability;

import com.otectus.spells_n_gods.data.GodDefinition;

public enum DivineTier {
    NONE(0),
    INITIATE(1),
    DEVOUT(2),
    EXALTED(3),
    ASCENDANT(4);

    private final int level;

    DivineTier(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isAtLeast(DivineTier other) {
        return this.level >= other.level;
    }

    public static DivineTier fromLevel(int level) {
        for (DivineTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return NONE;
    }

    public static DivineTier computeFromFavor(float favor, GodDefinition god) {
        if (god == null) {
            return NONE;
        }
        var thresholds = god.favor().thresholds();
        if (favor >= thresholds.ascendant()) return ASCENDANT;
        if (favor >= thresholds.exalted()) return EXALTED;
        if (favor >= thresholds.devout()) return DEVOUT;
        if (favor >= thresholds.initiate()) return INITIATE;
        return NONE;
    }

    public String getTierKey() {
        return switch (this) {
            case NONE -> "none";
            case INITIATE -> "initiate";
            case DEVOUT -> "devout";
            case EXALTED -> "exalted";
            case ASCENDANT -> "ascendant";
        };
    }
}
