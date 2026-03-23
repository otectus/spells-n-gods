package com.otectus.spells_n_gods.capability;

public enum BlessingState {
    UNBOUND,           // No god chosen
    ACTIVE,            // Bound, offerings recent, blessings apply
    HALTED,            // Bound but offerings lapsed, no blessings
    UNBOUND_COOLDOWN,  // After apostasy, during cooldown period
    MISSING_DEITY      // God definition removed from datapack
}
