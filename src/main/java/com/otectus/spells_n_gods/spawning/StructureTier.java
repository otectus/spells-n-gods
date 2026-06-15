package com.otectus.spells_n_gods.spawning;

import java.util.Locale;

/**
 * Tiered structure categories for the data-driven deity spawning system.
 *
 * <p>Tiers describe how rare and how dangerous a deity encounter at a structure is.
 * Each tier carries default tuning values (base chance, cooldown, active-deity budget,
 * and progression gates) which are configured in {@code structure_spawns.toml}.
 *
 * <p>This enum intentionally has <strong>no Minecraft dependencies</strong> so the spawn
 * decision logic that uses it can be unit-tested without a running game.
 */
public enum StructureTier {
    COMMON,
    UNCOMMON,
    RARE,
    LEGENDARY;

    /**
     * Parse a tier from a config/JSON string, falling back to {@code fallback} for any
     * unknown or blank value. Never throws — invalid values are the caller's concern to log.
     */
    public static StructureTier fromString(String raw, StructureTier fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return StructureTier.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** @return {@code true} if {@code raw} maps to a known tier (case-insensitive). */
    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            StructureTier.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
