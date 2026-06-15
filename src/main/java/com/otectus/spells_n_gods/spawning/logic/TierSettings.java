package com.otectus.spells_n_gods.spawning.logic;

import com.otectus.spells_n_gods.spawning.StructureTier;

/**
 * Immutable tuning for a single {@link StructureTier}, plus its progression gates.
 *
 * <p>Pure data — no Minecraft dependencies — so the spawn decision engine and its unit
 * tests can consume it directly.
 *
 * @param tier                 the tier these settings belong to
 * @param baseChance           base probability [0..1] that a deity encounter triggers
 * @param cooldownDays         in-game days before a structure may roll again;
 *                             {@code -1} means permanent exhaustion (never again)
 * @param maxActiveDeities     max simultaneously-active deity encounters per structure instance ({@code >= 1})
 * @param requiresDragonKilled if {@code true}, the triggering player must have completed
 *                             {@code minecraft:end/kill_dragon}
 * @param requiredAdvancement  advancement id the player must have completed, or {@code ""} for none
 * @param requiredDimension    dimension id the player must currently be in, or {@code ""} for none
 * @param minFavor             minimum favor (with the candidate deity) the player must hold, or {@code 0} for none
 */
public record TierSettings(
        StructureTier tier,
        double baseChance,
        int cooldownDays,
        int maxActiveDeities,
        boolean requiresDragonKilled,
        String requiredAdvancement,
        String requiredDimension,
        int minFavor
) {
    /** Sentinel cooldown value meaning the structure is exhausted permanently after one spawn. */
    public static final int PERMANENT_COOLDOWN = -1;

    public TierSettings {
        // Defensive normalisation — never let bad data crash the engine.
        if (baseChance < 0.0) baseChance = 0.0;
        if (baseChance > 1.0) baseChance = 1.0;
        if (maxActiveDeities < 1) maxActiveDeities = 1;
        if (cooldownDays < PERMANENT_COOLDOWN) cooldownDays = PERMANENT_COOLDOWN;
        if (minFavor < 0) minFavor = 0;
        if (requiredAdvancement == null) requiredAdvancement = "";
        if (requiredDimension == null) requiredDimension = "";
    }

    public boolean isPermanentExhaustion() {
        return cooldownDays == PERMANENT_COOLDOWN;
    }

    public boolean hasProgressionGate() {
        return requiresDragonKilled
                || !requiredAdvancement.isBlank()
                || !requiredDimension.isBlank()
                || minFavor > 0;
    }

    /** Sensible lore-leaning defaults used when no config is present (mirrors the task spec). */
    public static TierSettings defaultFor(StructureTier tier) {
        return switch (tier) {
            case COMMON -> new TierSettings(tier, 0.03, 20, 1, false, "", "", 0);
            case UNCOMMON -> new TierSettings(tier, 0.05, 30, 1, false, "", "", 0);
            case RARE -> new TierSettings(tier, 0.08, 60, 1, false, "", "", 0);
            case LEGENDARY -> new TierSettings(tier, 0.20, PERMANENT_COOLDOWN, 1, true, "", "", 0);
        };
    }
}
