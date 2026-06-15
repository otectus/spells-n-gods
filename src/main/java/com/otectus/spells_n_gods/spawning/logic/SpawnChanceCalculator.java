package com.otectus.spells_n_gods.spawning.logic;

import java.util.Locale;

/**
 * Pure implementation of the weighted condition stack that decides whether a deity
 * encounter triggers at a structure:
 *
 * <pre>
 *   base chance
 *     x structure tier modifier
 *     x biome/dimension modifier
 *     x deity weight modifier
 *     x world difficulty scalar
 *     (forced to 0 if progression gates are not satisfied)
 * </pre>
 *
 * <p>No Minecraft dependencies — fully unit-testable. The {@link Result} carries every
 * factor and a human-readable breakdown so debug logging can explain exactly why a deity
 * did or did not spawn.
 */
public final class SpawnChanceCalculator {
    /** Reference weight at which a deity's weight modifier is exactly 1.0. */
    public static final double REFERENCE_WEIGHT = 10.0;
    /** Clamp bounds for the deity weight modifier so extreme weights can't dominate the stack. */
    public static final double MIN_WEIGHT_MOD = 0.1;
    public static final double MAX_WEIGHT_MOD = 5.0;

    private SpawnChanceCalculator() {
    }

    /**
     * Outcome of a chance computation.
     *
     * @param finalChance     resolved probability in [0,1]
     * @param gatesPassed     whether progression gates were satisfied
     * @param breakdown       formatted multi-factor explanation for debug logs
     */
    public record Result(
            double baseChance,
            double tierModifier,
            double biomeDimModifier,
            double deityWeightModifier,
            double difficultyScalar,
            boolean gatesPassed,
            double finalChance,
            String breakdown
    ) {
    }

    /** Convert a raw selection weight into a clamped multiplicative modifier. */
    public static double weightModifier(int weight) {
        double mod = Math.max(1, weight) / REFERENCE_WEIGHT;
        return Math.max(MIN_WEIGHT_MOD, Math.min(MAX_WEIGHT_MOD, mod));
    }

    public static Result compute(
            double baseChance,
            double tierModifier,
            double biomeDimModifier,
            int deityWeight,
            double difficultyScalar,
            boolean gatesPassed) {

        double weightMod = weightModifier(deityWeight);
        double raw = baseChance * tierModifier * biomeDimModifier * weightMod * difficultyScalar;
        double finalChance = gatesPassed ? clamp01(raw) : 0.0;

        String breakdown = String.format(Locale.ROOT,
                "base=%.4f x tier=%.3f x biome/dim=%.3f x deityWeight=%.3f(w=%d) x difficulty=%.3f"
                        + " | gates=%s => final=%.4f",
                baseChance, tierModifier, biomeDimModifier, weightMod, deityWeight,
                difficultyScalar, gatesPassed ? "PASS" : "FAIL", finalChance);

        return new Result(baseChance, tierModifier, biomeDimModifier, weightMod,
                difficultyScalar, gatesPassed, finalChance, breakdown);
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
