package com.otectus.spells_n_gods.boss.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossSiegeLogicTest {

    // --- decideMode ---

    @Test
    void noTarget_yieldsNone() {
        assertEquals(BossSiegeLogic.Mode.NONE,
                BossSiegeLogic.decideMode(false, true, true, 100, 5));
    }

    @Test
    void disabled_yieldsNone() {
        assertEquals(BossSiegeLogic.Mode.NONE,
                BossSiegeLogic.decideMode(true, false, true, 100, 5));
    }

    @Test
    void clearPath_yieldsNone() {
        assertEquals(BossSiegeLogic.Mode.NONE,
                BossSiegeLogic.decideMode(true, true, false, 100, 5));
    }

    @Test
    void obstructedBelowThreshold_breaksPath() {
        assertEquals(BossSiegeLogic.Mode.BREAK_PATH,
                BossSiegeLogic.decideMode(true, true, true, 4, 5));
    }

    @Test
    void obstructedAtOrAboveThreshold_escalatesToArea() {
        assertEquals(BossSiegeLogic.Mode.BREAK_AREA,
                BossSiegeLogic.decideMode(true, true, true, 5, 5));
        assertEquals(BossSiegeLogic.Mode.BREAK_AREA,
                BossSiegeLogic.decideMode(true, true, true, 99, 5));
    }

    // --- updateStuckTicks ---

    @Test
    void notObstructed_resetsStuck() {
        assertEquals(0, BossSiegeLogic.updateStuckTicks(40, false, 100, 100));
    }

    @Test
    void obstructedAndClosing_resetsStuck() {
        // distance shrank well beyond the progress epsilon
        assertEquals(0, BossSiegeLogic.updateStuckTicks(40, true, 100.0, 90.0));
    }

    @Test
    void obstructedAndNotClosing_incrementsStuck() {
        assertEquals(11, BossSiegeLogic.updateStuckTicks(10, true, 100.0, 100.0));
        // negligible progress (below epsilon) still counts as stuck
        assertEquals(11, BossSiegeLogic.updateStuckTicks(10, true, 100.0, 99.9));
    }

    // --- stepDirection ---

    @Test
    void cardinalDirections() {
        assertEquals(1, BossSiegeLogic.stepDirection(5.0, 0.0)[0]);
        assertEquals(0, BossSiegeLogic.stepDirection(5.0, 0.0)[1]);
        assertEquals(-1, BossSiegeLogic.stepDirection(0.0, -5.0)[1]);
    }

    @Test
    void tinyDeltaIsZero() {
        int[] d = BossSiegeLogic.stepDirection(0.1, -0.1);
        assertEquals(0, d[0]);
        assertEquals(0, d[1]);
    }

    @Test
    void diagonalDirection() {
        int[] d = BossSiegeLogic.stepDirection(-4.0, 3.0);
        assertEquals(-1, d[0]);
        assertEquals(1, d[1]);
    }

    // --- pathBreakOffsets ---

    @Test
    void noDirection_noOffsets() {
        assertTrue(BossSiegeLogic.pathBreakOffsets(0, 0, 3).isEmpty());
    }

    @Test
    void cardinalPath_isSingleColumnOfHeight() {
        List<int[]> offsets = BossSiegeLogic.pathBreakOffsets(1, 0, 3);
        assertEquals(3, offsets.size());
        // all share the same horizontal offset, varying y across 0..2
        for (int[] o : offsets) {
            assertEquals(1, o[0]);
            assertEquals(0, o[2]);
        }
        assertTrue(offsets.stream().anyMatch(o -> o[1] == 0));
        assertTrue(offsets.stream().anyMatch(o -> o[1] == 2));
    }

    @Test
    void diagonalPath_clearsThreeColumns() {
        // diagonal => front column + two axis-aligned neighbours, each `height` tall
        List<int[]> offsets = BossSiegeLogic.pathBreakOffsets(1, 1, 3);
        assertEquals(9, offsets.size());
        assertTrue(offsets.stream().anyMatch(o -> o[0] == 1 && o[2] == 1));
        assertTrue(offsets.stream().anyMatch(o -> o[0] == 1 && o[2] == 0));
        assertTrue(offsets.stream().anyMatch(o -> o[0] == 0 && o[2] == 1));
    }

    // --- areaBreakOffsets ---

    @Test
    void areaSmash_isBoxMinusOwnColumn() {
        int radius = 2;
        int height = 3;
        List<int[]> offsets = BossSiegeLogic.areaBreakOffsets(radius, height);
        int side = radius * 2 + 1; // 5
        int expected = (side * side - 1) * height; // exclude own column (x=0,z=0)
        assertEquals(expected, offsets.size());
        // never includes the boss's own vertical column
        assertFalse(offsets.stream().anyMatch(o -> o[0] == 0 && o[2] == 0));
    }
}
