package com.otectus.spells_n_gods.spawning.logic;

import java.util.List;
import java.util.Random;

/**
 * Deterministic weighted selection helper used to pick which deity spawns at a structure
 * when several are eligible. Pure logic — accepts an injected {@link Random} so callers can
 * seed it for reproducibility/debuggability and tests can assert exact outcomes.
 */
public final class WeightedSelection {
    private WeightedSelection() {
    }

    /** A candidate with a non-negative selection weight. */
    public record Weighted<T>(T value, int weight) {
    }

    /**
     * Pick one candidate proportional to weight.
     *
     * @return the selected value, or {@code null} if the list is empty or all weights are {@code <= 0}
     */
    public static <T> T pick(List<Weighted<T>> candidates, Random random) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        long total = 0L;
        for (Weighted<T> c : candidates) {
            if (c.weight() > 0) {
                total += c.weight();
            }
        }
        if (total <= 0L) {
            return null;
        }
        long roll = (long) (random.nextDouble() * total); // [0, total)
        long cursor = 0L;
        for (Weighted<T> c : candidates) {
            if (c.weight() <= 0) {
                continue;
            }
            cursor += c.weight();
            if (roll < cursor) {
                return c.value();
            }
        }
        // Floating-point edge case: return the last positive-weight entry.
        for (int i = candidates.size() - 1; i >= 0; i--) {
            if (candidates.get(i).weight() > 0) {
                return candidates.get(i).value();
            }
        }
        return null;
    }

    public static long totalWeight(List<? extends Weighted<?>> candidates) {
        long total = 0L;
        for (Weighted<?> c : candidates) {
            if (c.weight() > 0) {
                total += c.weight();
            }
        }
        return total;
    }
}
