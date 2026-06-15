package com.otectus.spells_n_gods.spawning;

import java.util.Locale;

/**
 * Data-driven behaviour variance for a deity at the moment it spawns at a structure. This lets
 * the same deity behave differently depending on structure/domain (e.g. a war god is
 * {@link #NEUTRAL} at a war camp but {@link #HOSTILE} at a rival's library).
 *
 * <p>Integration is intentionally lightweight: alignments map onto the existing boss AI by
 * controlling whether the deity is combat-active or dormant on arrival. Finer behaviour
 * (faction targeting, dialogue, etc.) is left as a documented extension point.
 */
public enum SpawnAlignment {
    /** Full boss AI, immediately combat-ready. */
    HOSTILE(true),
    /** Boss AI enabled but does not pre-aggro; reacts if attacked. */
    NEUTRAL(true),
    /** Present but non-combative unless provoked. */
    PASSIVE(false),
    /** Same as hostile but framed as an optional challenge encounter. */
    CHALLENGE(true),
    /** Inert until disturbed (no AI on arrival). */
    DORMANT(false),
    /** Will not act on its own; intended to be triggered by an external summon/ritual. */
    SUMMON_ONLY(false);

    private final boolean combatActiveOnSpawn;

    SpawnAlignment(boolean combatActiveOnSpawn) {
        this.combatActiveOnSpawn = combatActiveOnSpawn;
    }

    /** Whether the deity should have AI enabled the instant its spawn sequence completes. */
    public boolean isCombatActiveOnSpawn() {
        return combatActiveOnSpawn;
    }

    public static SpawnAlignment fromString(String raw, SpawnAlignment fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return SpawnAlignment.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
