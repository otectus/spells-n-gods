package com.otectus.spells_n_gods.durability.logic;

/**
 * Pure, deterministic math for scaling item durability damage by the player's apostasy-scar penalty
 * and Aurex/Permanence blessing multipliers.
 *
 * <p>This class intentionally has <b>no Minecraft dependencies</b> so it is fully unit-testable in
 * isolation (mirroring {@code spawning.logic.*}). All Minecraft-facing resolution and guarding lives
 * in {@code com.otectus.spells_n_gods.durability.DurabilityDamageHandler}, which delegates the actual
 * arithmetic here.
 *
 * <p><b>Multiplier conventions:</b> a scar penalty multiplier is {@code >= 1.0} (items wear faster);
 * a blessing multiplier is {@code <= 1.0} (items wear slower). They compose multiplicatively.
 */
public final class DurabilityModifierService {

    private DurabilityModifierService() {}

    /**
     * Combine the scar-penalty and blessing multipliers into a single factor applied to the damage
     * amount. Multiplicative composition keeps the two systems independent and order-independent.
     * Clamped to {@code >= 0} for safety against malformed data.
     */
    public static float combineMultipliers(float scarMultiplier, float blessingMultiplier) {
        float combined = scarMultiplier * blessingMultiplier;
        return combined < 0f ? 0f : combined;
    }

    /**
     * Apply a (possibly fractional) multiplier to an integer durability-damage amount using
     * probabilistic rounding, so fractional factors are meaningful even for the common 1-point hit.
     *
     * <p>The whole part of {@code baseAmount * multiplier} is always applied; the fractional
     * remainder becomes the probability of one additional point. The caller supplies the random draw
     * ({@code rng} in {@code [0, 1)}) so the result is deterministic and testable.
     *
     * <ul>
     *   <li>{@code multiplier == 1.0} or {@code baseAmount <= 0} → returned unchanged (vanilla).</li>
     *   <li>e.g. {@code applyMultiplier(1, 1.5f, rng)} → {@code 2} when {@code rng < 0.5}, else {@code 1}.</li>
     *   <li>e.g. {@code applyMultiplier(1, 0.5f, rng)} → {@code 0} when {@code rng >= 0.5}, else {@code 1}.</li>
     * </ul>
     *
     * @param baseAmount the vanilla durability damage (typically 1)
     * @param multiplier the combined scar/blessing factor
     * @param rng        a random value in {@code [0, 1)}
     * @return the scaled durability damage, never negative
     */
    public static int applyMultiplier(int baseAmount, float multiplier, double rng) {
        if (baseAmount <= 0 || multiplier == 1.0f) {
            return baseAmount;
        }

        double scaled = baseAmount * (double) multiplier;
        int whole = (int) Math.floor(scaled);
        double fraction = scaled - whole;

        if (fraction > 0.0 && rng < fraction) {
            whole++;
        }

        return Math.max(0, whole);
    }
}
