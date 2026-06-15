package com.otectus.spells_n_gods.spawning.logic;

import com.otectus.spells_n_gods.spawning.StructureTier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure (Minecraft-free) deity structure-spawning decision logic.
 *
 * <p>Each test maps to one of the system's required behaviours. The Minecraft-coupled pieces
 * (NBT persistence, structure detection, gate evaluation against a live player) are exercised
 * in-game; this suite locks down the deterministic decision core.
 */
class SpawnLogicTest {

    // 1. Tier defaults apply correctly.
    @Test
    void tierDefaultsApply() {
        TierSettings common = TierSettings.defaultFor(StructureTier.COMMON);
        assertEquals(0.03, common.baseChance(), 1e-9);
        assertEquals(20, common.cooldownDays());
        assertEquals(1, common.maxActiveDeities());
        assertFalse(common.hasProgressionGate());

        TierSettings legendary = TierSettings.defaultFor(StructureTier.LEGENDARY);
        assertEquals(0.20, legendary.baseChance(), 1e-9);
        assertTrue(legendary.isPermanentExhaustion());
        assertTrue(legendary.requiresDragonKilled());
        assertTrue(legendary.hasProgressionGate());
    }

    @Test
    void tierParsingIsLenient() {
        assertEquals(StructureTier.RARE, StructureTier.fromString("RaRe", StructureTier.COMMON));
        assertEquals(StructureTier.COMMON, StructureTier.fromString("nonsense", StructureTier.COMMON));
        assertEquals(StructureTier.COMMON, StructureTier.fromString(null, StructureTier.COMMON));
        assertFalse(StructureTier.isValid("mythic"));
        assertTrue(StructureTier.isValid("legendary"));
    }

    // 2. Structure-specific overrides beat tier/tag defaults (deity weight precedence).
    @Test
    void whitelistWeightBeatsTagDefault() {
        DeityStructureProfile profile = new DeityStructureProfile(
                "spells_n_gods:deus", StructureTier.RARE, false,
                List.of("#spells_n_gods:wisdom_structures"),
                Map.of("minecraft:stronghold", 25));

        // Explicit whitelist entry wins regardless of tag match.
        assertEquals(25, profile.weightFor("minecraft:stronghold", true));
        assertEquals(25, profile.weightFor("minecraft:stronghold", false));
        // Tag-matched but no explicit entry -> default tag weight.
        assertEquals(DeityStructureProfile.DEFAULT_TAG_WEIGHT,
                profile.weightFor("minecraft:village_plains", true));
        // Neither whitelisted nor tag-matched -> not applicable.
        assertEquals(0, profile.weightFor("minecraft:igloo", false));
    }

    // 3. Domain tags resolve correctly (applies-to logic incl. allow_any and disable_default).
    @Test
    void domainTagResolution() {
        DeityStructureProfile tagged = new DeityStructureProfile(
                "a", StructureTier.RARE, false,
                List.of("#ns:wisdom_structures"), Map.of());
        assertTrue(tagged.appliesTo("minecraft:stronghold", true, false));   // tag match
        assertFalse(tagged.appliesTo("minecraft:stronghold", false, false)); // no tag, no any
        assertTrue(tagged.appliesTo("minecraft:stronghold", false, true));   // allow_any

        DeityStructureProfile whitelistOnly = new DeityStructureProfile(
                "b", StructureTier.RARE, true /* disable_default */,
                List.of("#ns:wisdom_structures"), Map.of("minecraft:end_city", 40));
        assertFalse(whitelistOnly.appliesTo("minecraft:stronghold", true, true)); // tag/any ignored
        assertTrue(whitelistOnly.appliesTo("minecraft:end_city", false, false));  // explicit only
    }

    // 4. Deity weights produce a valid weighted selection.
    @Test
    void weightedSelectionRespectsWeights() {
        List<WeightedSelection.Weighted<String>> candidates = List.of(
                new WeightedSelection.Weighted<>("low", 1),
                new WeightedSelection.Weighted<>("high", 99));
        int low = 0, high = 0;
        Random rng = new Random(1234L);
        for (int i = 0; i < 10_000; i++) {
            String pick = WeightedSelection.pick(candidates, rng);
            if ("low".equals(pick)) low++;
            else if ("high".equals(pick)) high++;
        }
        assertTrue(high > low * 10, "heavy weight should dominate (high=" + high + ", low=" + low + ")");

        assertEquals(100L, WeightedSelection.totalWeight(candidates));
        // All-zero weights select nothing.
        assertNull(WeightedSelection.pick(
                List.of(new WeightedSelection.Weighted<>("x", 0)), rng));
    }

    // 5. Cooldowns prevent repeated spawning.
    @Test
    void cooldownPreventsRepeatSpawn() {
        CooldownState state = CooldownState.fresh();
        assertTrue(state.canSpawn(0L, 1));

        // Spawn with a 60-day cooldown.
        CooldownState after = state.withSpawn(1000L, 60);
        assertFalse(after.canSpawn(1000L, 1), "budget full + on cooldown");
        // Free the budget slot but cooldown is still active.
        CooldownState freed = after.withDeityRemoved();
        assertFalse(freed.canSpawn(1000L + 60 * CooldownState.TICKS_PER_DAY - 1, 1));
        // Once the cooldown elapses it may spawn again.
        assertTrue(freed.canSpawn(1000L + 60 * CooldownState.TICKS_PER_DAY, 1));
    }

    // 6. Permanent exhaustion works.
    @Test
    void permanentExhaustion() {
        CooldownState exhausted = CooldownState.fresh().withSpawn(500L, -1).withDeityRemoved();
        assertTrue(exhausted.isPermanentlyExhausted());
        assertFalse(exhausted.canSpawn(Long.MAX_VALUE - 1, 8), "never spawns again");
        assertFalse(exhausted.isPrunable(Long.MAX_VALUE - 1), "permanent records are kept, not pruned");
    }

    // 7. Spawn budgets prevent multiple active deities in one structure.
    @Test
    void spawnBudgetEnforced() {
        // Cooldown 0 days so only the budget can block.
        CooldownState s0 = CooldownState.fresh();
        assertTrue(s0.canSpawn(10_000L, 1));
        CooldownState s1 = s0.withSpawn(10_000L, 0);
        assertEquals(1, s1.activeDeityCount());
        assertFalse(s1.canSpawn(10_000L, 1), "budget of 1 is consumed");
        assertTrue(s1.canSpawn(10_000L, 2), "budget of 2 still allows another");
    }

    @Test
    void prunableOnlyWhenIdleAndCooldownElapsed() {
        CooldownState active = CooldownState.fresh().withSpawn(0L, 10);
        assertFalse(active.isPrunable(0L), "active deity present");
        CooldownState idle = active.withDeityRemoved();
        assertFalse(idle.isPrunable(5L), "cooldown not elapsed");
        assertTrue(idle.isPrunable(10L * CooldownState.TICKS_PER_DAY), "idle + elapsed = prunable");
    }

    // 8. Invalid config entries warn but do not crash.
    @Test
    void invalidConfigWarnsNeverThrows() {
        DeityStructureProfile bad = new DeityStructureProfile(
                "unknown:deity", StructureTier.RARE, true,
                List.of("not-a-tag", "#ns:ok"),
                Map.of("bad id with spaces", -5));
        List<String> warnings = ConfigValidation.validateProfile(bad, Set.<String>of());
        assertFalse(warnings.isEmpty(), "malformed profile should produce warnings");

        // Malformed tier override warns rather than throwing.
        assertFalse(ConfigValidation.validateOverride("minecraft:stronghold=mythic").isEmpty());
        assertFalse(ConfigValidation.validateOverride("garbage").isEmpty());
        assertTrue(ConfigValidation.validateOverride("minecraft:stronghold=rare").isEmpty());

        assertTrue(ConfigValidation.isValidResourceId("minecraft:stronghold"));
        assertFalse(ConfigValidation.isValidResourceId("Stronghold"));
        assertTrue(ConfigValidation.isValidTagReference("#ns:tag"));
        assertFalse(ConfigValidation.isValidTagReference("ns:tag"));
    }

    // 9. Progression gates block (legendary) spawns until satisfied.
    @Test
    void failedGatesForceZeroChance() {
        SpawnChanceCalculator.Result blocked = SpawnChanceCalculator.compute(
                0.20, 1.0, 1.0, 40, 1.0, /*gatesPassed*/ false);
        assertEquals(0.0, blocked.finalChance(), 1e-9, "failed gate => no spawn");

        SpawnChanceCalculator.Result allowed = SpawnChanceCalculator.compute(
                0.20, 1.0, 1.0, 40, 1.0, /*gatesPassed*/ true);
        assertTrue(allowed.finalChance() > 0.0);
        assertTrue(allowed.breakdown().contains("PASS"));
    }

    // Condition stack ordering & clamping behave deterministically.
    @Test
    void chanceStackMultipliesAndClamps() {
        // weightModifier(10) == 1.0 (reference weight)
        assertEquals(1.0, SpawnChanceCalculator.weightModifier(10), 1e-9);
        // Heavy weight is clamped to MAX_WEIGHT_MOD.
        assertEquals(SpawnChanceCalculator.MAX_WEIGHT_MOD,
                SpawnChanceCalculator.weightModifier(10_000), 1e-9);

        SpawnChanceCalculator.Result r = SpawnChanceCalculator.compute(
                0.5, 1.0, 1.0, 10, 0.5, true);
        assertEquals(0.25, r.finalChance(), 1e-9);

        // Never exceeds 1.0.
        SpawnChanceCalculator.Result clamped = SpawnChanceCalculator.compute(
                0.9, 2.0, 2.0, 50, 2.0, true);
        assertEquals(1.0, clamped.finalChance(), 1e-9);
    }

    // 10. Saved-data state survives a value round-trip (proxy for NBT persistence).
    @Test
    void cooldownStateReconstructsFaithfully() {
        CooldownState original = CooldownState.fresh().withSpawn(7777L, 30);
        CooldownState rebuilt = new CooldownState(
                original.lastSpawnGameTime(), original.cooldownEndsGameTime(),
                original.spawnedCount(), original.activeDeityCount());
        assertEquals(original, rebuilt, "records reconstruct identically from their fields");
        assertEquals(7777L + 30 * CooldownState.TICKS_PER_DAY, rebuilt.cooldownEndsGameTime());
        assertEquals(1, rebuilt.spawnedCount());
    }
}
