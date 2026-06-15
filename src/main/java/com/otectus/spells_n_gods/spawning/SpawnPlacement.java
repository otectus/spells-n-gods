package com.otectus.spells_n_gods.spawning;

import java.util.Locale;

/**
 * Data-driven spawn placement strategies for resolving where, inside a detected structure's
 * bounding box, a deity should appear. The actual block-scanning lives in
 * {@code PlacementResolver}; this enum is the configurable selector.
 */
public enum SpawnPlacement {
    /** Geometric centre of the structure bounding box, adjusted to a safe Y. */
    CENTER_OF_STRUCTURE,
    /** Down-scan from the centre to the first solid floor with breathable space above it. */
    NEAREST_SAFE_FLOOR,
    /** Like {@link #NEAREST_SAFE_FLOOR} but standing in the air column directly above the floor. */
    NEAREST_AIR_ABOVE_FLOOR,
    /** A random valid (safe) position within the bounding box. */
    RANDOM_VALID_POSITION,
    /** Use a shrine/altar anchor block if one is present, else fall back to {@link #NEAREST_SAFE_FLOOR}. */
    SHRINE_ANCHOR;

    public static SpawnPlacement fromString(String raw, SpawnPlacement fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return SpawnPlacement.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
