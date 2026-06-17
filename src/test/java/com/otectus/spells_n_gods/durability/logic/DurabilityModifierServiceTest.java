package com.otectus.spells_n_gods.durability.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the pure durability-scaling math. No Minecraft classes involved — the service is
 * deterministic given an explicit random draw, so every branch is exercised directly.
 */
class DurabilityModifierServiceTest {

    @Test
    void combineIsMultiplicativeAndOrderIndependent() {
        assertEquals(1.5f, DurabilityModifierService.combineMultipliers(1.5f, 1.0f), 1e-6);
        assertEquals(0.5f, DurabilityModifierService.combineMultipliers(1.0f, 0.5f), 1e-6);
        // scar 1.5 with blessing 0.5 -> 0.75
        assertEquals(0.75f, DurabilityModifierService.combineMultipliers(1.5f, 0.5f), 1e-6);
        assertEquals(
                DurabilityModifierService.combineMultipliers(1.5f, 0.5f),
                DurabilityModifierService.combineMultipliers(0.5f, 1.5f),
                1e-6);
    }

    @Test
    void combineClampsNegativeToZero() {
        assertEquals(0f, DurabilityModifierService.combineMultipliers(-2f, 1f), 1e-6);
    }

    @Test
    void identityMultiplierLeavesAmountUnchanged() {
        // Any rng must not matter when multiplier is exactly 1.0.
        assertEquals(1, DurabilityModifierService.applyMultiplier(1, 1.0f, 0.0));
        assertEquals(5, DurabilityModifierService.applyMultiplier(5, 1.0f, 0.999));
    }

    @Test
    void nonPositiveBaseReturnsUnchanged() {
        assertEquals(0, DurabilityModifierService.applyMultiplier(0, 1.5f, 0.0));
        assertEquals(-3, DurabilityModifierService.applyMultiplier(-3, 1.5f, 0.0));
    }

    @Test
    void scarPenaltyProbabilisticallyAddsAPoint() {
        // 1 * 1.5 = 1.5 -> whole 1, fraction 0.5
        assertEquals(2, DurabilityModifierService.applyMultiplier(1, 1.5f, 0.0));   // rng < 0.5 -> +1
        assertEquals(2, DurabilityModifierService.applyMultiplier(1, 1.5f, 0.49));
        assertEquals(1, DurabilityModifierService.applyMultiplier(1, 1.5f, 0.5));   // rng >= 0.5 -> floor
        assertEquals(1, DurabilityModifierService.applyMultiplier(1, 1.5f, 0.99));
    }

    @Test
    void blessingProbabilisticallyRemovesThePoint() {
        // 1 * 0.5 = 0.5 -> whole 0, fraction 0.5
        assertEquals(1, DurabilityModifierService.applyMultiplier(1, 0.5f, 0.0));   // rng < 0.5 -> +1
        assertEquals(0, DurabilityModifierService.applyMultiplier(1, 0.5f, 0.5));   // rng >= 0.5 -> 0
        assertEquals(0, DurabilityModifierService.applyMultiplier(1, 0.5f, 0.99));
    }

    @Test
    void rngEndpointsMapToFloorAndCeil() {
        // 3 * 1.25 = 3.75 -> whole 3, fraction 0.75
        assertEquals(4, DurabilityModifierService.applyMultiplier(3, 1.25f, 0.0));  // rng=0 -> always +1
        assertEquals(3, DurabilityModifierService.applyMultiplier(3, 1.25f, 0.75)); // rng==fraction -> floor
        assertEquals(3, DurabilityModifierService.applyMultiplier(3, 1.25f, 0.999));
    }

    @Test
    void wholeMultiplierNeedsNoRandomness() {
        // 2 * 2.0 = 4.0 -> fraction 0, rng irrelevant
        assertEquals(4, DurabilityModifierService.applyMultiplier(2, 2.0f, 0.0));
        assertEquals(4, DurabilityModifierService.applyMultiplier(2, 2.0f, 0.999));
    }

    @Test
    void combinedScarAndBlessingScalesAmount() {
        // combined 0.75 on a 4-point hit -> 3.0 exactly
        float combined = DurabilityModifierService.combineMultipliers(1.5f, 0.5f);
        assertEquals(3, DurabilityModifierService.applyMultiplier(4, combined, 0.0));
        assertEquals(3, DurabilityModifierService.applyMultiplier(4, combined, 0.999));
    }

    @Test
    void resultNeverNegative() {
        assertEquals(0, DurabilityModifierService.applyMultiplier(1, 0.0f, 0.999));
    }

    @Test
    void deterministicForEqualInputs() {
        int a = DurabilityModifierService.applyMultiplier(2, 1.37f, 0.42);
        int b = DurabilityModifierService.applyMultiplier(2, 1.37f, 0.42);
        assertEquals(a, b);
    }
}
