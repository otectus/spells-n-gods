package com.otectus.spells_n_gods.boss;

public enum BossPhase {
    IDLE,
    COMBAT,
    ENRAGED;

    public boolean isEnraged() {
        return this == ENRAGED;
    }

    public boolean isInCombat() {
        return this == COMBAT || this == ENRAGED;
    }
}
